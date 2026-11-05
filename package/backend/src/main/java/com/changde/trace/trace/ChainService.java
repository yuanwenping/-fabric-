package com.changde.trace.trace;

import com.changde.trace.api.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChainService {
    private static final Logger LOG = LoggerFactory.getLogger(ChainService.class);
    private final FabricProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, FabricConnection> connections = new ConcurrentHashMap<>();

    public ChainService(FabricProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public TraceModels.ChainSubmission submit(String mspId, String function, String... args) {
        if (!properties.isEnabled()) {
            return new TraceModels.ChainSubmission("FABRIC_DISABLED", null);
        }
        try {
            FabricConnection connection = connection(mspId);
            TransactionProposalRequest request = connection.client().newTransactionProposalRequest();
            request.setChaincodeName(properties.getChaincode());
            request.setFcn(function);
            request.setArgs(args);
            request.setProposalWaitTime(20_000);

            Collection<ProposalResponse> responses =
                    connection.channel().sendTransactionProposal(request, Set.of(connection.peer()));
            for (ProposalResponse response : responses) {
                if (!response.isVerified() || response.getStatus() != ProposalResponse.Status.SUCCESS) {
                    throw new IllegalStateException(response.getMessage());
                }
            }
            BlockEvent.TransactionEvent event = connection.channel()
                    .sendTransaction(responses)
                    .get(30, java.util.concurrent.TimeUnit.SECONDS);
            if (event == null || !event.isValid()) {
                throw new IllegalStateException("transaction was not committed as VALID");
            }
            return new TraceModels.ChainSubmission(
                    event.getTransactionID(),
                    event.getBlockEvent().getBlockNumber()
            );
        } catch (Exception ex) {
            LOG.error("Fabric submit failed for MSP {} and function {}", mspId, function, ex);
            throw ApiException.chainError("区块链提交失败：" + rootMessage(ex));
        }
    }

    public JsonNode evaluate(String mspId, String function, String... args) {
        if (!properties.isEnabled()) {
            return objectMapper.createObjectNode().put("fabricEnabled", false);
        }
        try {
            FabricConnection connection = connection(mspId);
            QueryByChaincodeRequest request = connection.client().newQueryProposalRequest();
            request.setChaincodeName(properties.getChaincode());
            request.setFcn(function);
            request.setArgs(args);
            request.setProposalWaitTime(15_000);
            Collection<ProposalResponse> responses =
                    connection.channel().queryByChaincode(request, Set.of(connection.peer()));
            for (ProposalResponse response : responses) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    return objectMapper.readTree(response.getChaincodeActionResponsePayload());
                }
            }
            throw new IllegalStateException("chaincode query returned no successful response");
        } catch (Exception ex) {
            LOG.error("Fabric query failed for MSP {} and function {}", mspId, function, ex);
            throw ApiException.chainError("区块链查询失败：" + rootMessage(ex));
        }
    }

    public boolean verify(String eventId, String hash) {
        if (!properties.isEnabled()) {
            return false;
        }
        JsonNode result = evaluate("SupplierMSP", "VerifyEvent", eventId, hash);
        return result.isBoolean() && result.asBoolean();
    }

    public String status() {
        if (!properties.isEnabled()) {
            return "DISABLED";
        }
        try {
            evaluate("SupplierMSP", "GetBatch", "__healthcheck__");
            return "CONNECTED";
        } catch (ApiException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("does not exist") || message.contains("no successful response")) {
                return "CONNECTED";
            }
            return "ERROR: " + message;
        }
    }

    @PreDestroy
    public void close() {
        connections.values().forEach(FabricConnection::close);
    }

    private FabricConnection connection(String mspId) {
        FabricProperties.Organization organization = properties.getOrganizations().get(mspId);
        if (organization == null) {
            throw new IllegalArgumentException("Fabric 身份未配置：" + mspId);
        }
        return connections.computeIfAbsent(mspId, ignored -> {
            try {
                return createConnection(mspId, organization);
            } catch (Exception ex) {
                throw new IllegalStateException("无法连接 " + mspId + "：" + rootMessage(ex), ex);
            }
        });
    }

    private FabricConnection createConnection(
            String mspId,
            FabricProperties.Organization organization
    ) throws Exception {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(new FabricUser(
                mspId,
                read(organization.getCert()),
                privateKey(read(organization.getKey()))
        ));

        Peer peer = client.newPeer(
                organization.getAuthority(),
                organization.getEndpoint(),
                tlsProperties(organization.getTls(), organization.getAuthority())
        );
        Orderer orderer = client.newOrderer(
                properties.getOrdererAuthority(),
                properties.getEndpointOrderer(),
                tlsProperties(properties.getOrdererTls(), properties.getOrdererAuthority())
        );

        Channel channel = client.newChannel(properties.getChannel());
        channel.addPeer(peer);
        channel.addOrderer(orderer);
        channel.initialize();
        return new FabricConnection(client, channel, peer);
    }

    private Properties tlsProperties(Resource tlsResource, String authority) throws IOException {
        Properties properties = new Properties();
        properties.put("pemBytes", tlsResource.getInputStream().readAllBytes());
        properties.setProperty("hostnameOverride", authority);
        properties.setProperty("sslProvider", "openSSL");
        properties.setProperty("negotiationType", "TLS");
        return properties;
    }

    private String read(Resource resource) throws IOException {
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private PrivateKey privateKey(String pem) throws Exception {
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        for (String algorithm : List.of("EC", "RSA")) {
            try {
                return java.security.KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
            } catch (Exception ignored) {
                // Try the next supported key algorithm.
            }
        }
        throw new IllegalArgumentException("unsupported private key format");
    }

    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record FabricConnection(HFClient client, Channel channel, Peer peer) {
        private void close() {
            channel.shutdown(true);
        }
    }

    private static final class FabricUser implements User {
        private final String name;
        private final String mspId;
        private final String certificate;
        private final Enrollment enrollment;

        private FabricUser(String mspId, String certificate, PrivateKey privateKey) {
            this.name = "trace-client-" + mspId;
            this.mspId = mspId;
            this.certificate = certificate;
            this.enrollment = new Enrollment() {
                @Override
                public PrivateKey getKey() {
                    return privateKey;
                }

                @Override
                public String getCert() {
                    return certificate;
                }
            };
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Set<String> getRoles() {
            return Set.of();
        }

        @Override
        public String getAccount() {
            return name;
        }

        @Override
        public String getAffiliation() {
            return "";
        }

        @Override
        public Enrollment getEnrollment() {
            return enrollment;
        }

        @Override
        public String getMspId() {
            return mspId;
        }
    }
}
