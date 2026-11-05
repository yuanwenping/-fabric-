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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Service
public class ChainService {
    private static final Logger LOG = LoggerFactory.getLogger(ChainService.class);

    private final boolean enabled;
    private final String channelName;
    private final String chaincodeName;
    private final String endpoint;
    private final String ordererEndpoint;
    private final String mspId;
    private final String peerAuthority;
    private final String ordererAuthority;
    private final Resource clientCert;
    private final Resource clientKey;
    private final Resource peerTls;
    private final Resource ordererTls;
    private final ObjectMapper objectMapper;
    private FabricConnection connection;

    public ChainService(
            @Value("${fabric.enabled:true}") boolean enabled,
            @Value("${fabric.channel}") String channelName,
            @Value("${fabric.chaincode}") String chaincodeName,
            @Value("${fabric.endpoint}") String endpoint,
            @Value("${fabric.endpoint-orderer}") String ordererEndpoint,
            @Value("${fabric.msp-id}") String mspId,
            @Value("${fabric.peer-authority}") String peerAuthority,
            @Value("${fabric.orderer-authority}") String ordererAuthority,
            @Value("${fabric.client-cert}") Resource clientCert,
            @Value("${fabric.client-key}") Resource clientKey,
            @Value("${fabric.peer-tls}") Resource peerTls,
            @Value("${fabric.orderer-tls}") Resource ordererTls,
            ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.channelName = channelName;
        this.chaincodeName = chaincodeName;
        this.endpoint = endpoint;
        this.ordererEndpoint = ordererEndpoint;
        this.mspId = mspId;
        this.peerAuthority = peerAuthority;
        this.ordererAuthority = ordererAuthority;
        this.clientCert = clientCert;
        this.clientKey = clientKey;
        this.peerTls = peerTls;
        this.ordererTls = ordererTls;
        this.objectMapper = objectMapper;
    }

    public TraceModels.ChainSubmission submit(String ignoredMspId, String function, String... args) {
        if (!enabled) {
            return new TraceModels.ChainSubmission("FABRIC_DISABLED", null);
        }
        try {
            FabricConnection current = connection();
            TransactionProposalRequest request = current.client().newTransactionProposalRequest();
            request.setChaincodeName(chaincodeName);
            request.setFcn(function);
            request.setArgs(args);
            request.setProposalWaitTime(20_000);

            Collection<ProposalResponse> responses = current.channel()
                    .sendTransactionProposal(request, Set.of(current.peer()));
            for (ProposalResponse response : responses) {
                if (!response.isVerified() || response.getStatus() != ProposalResponse.Status.SUCCESS) {
                    throw new IllegalStateException(response.getMessage());
                }
            }
            BlockEvent.TransactionEvent event = current.channel()
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
            LOG.error("Fabric submit failed for function {}", function, ex);
            throw ApiException.chainError("区块链提交失败：" + rootMessage(ex));
        }
    }

    public JsonNode evaluate(String ignoredMspId, String function, String... args) {
        if (!enabled) {
            return objectMapper.createObjectNode().put("fabricEnabled", false);
        }
        try {
            FabricConnection current = connection();
            QueryByChaincodeRequest request = current.client().newQueryProposalRequest();
            request.setChaincodeName(chaincodeName);
            request.setFcn(function);
            request.setArgs(args);
            request.setProposalWaitTime(15_000);
            Collection<ProposalResponse> responses =
                    current.channel().queryByChaincode(request, Set.of(current.peer()));
            for (ProposalResponse response : responses) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    return objectMapper.readTree(response.getChaincodeActionResponsePayload());
                }
            }
            throw new IllegalStateException("chaincode query returned no successful response");
        } catch (Exception ex) {
            LOG.error("Fabric query failed for function {}", function, ex);
            throw ApiException.chainError("区块链查询失败：" + rootMessage(ex));
        }
    }

    public boolean verify(String eventId, String hash) {
        if (!enabled) {
            return false;
        }
        JsonNode result = evaluate(mspId, "VerifyEvent", eventId, hash);
        return result.isBoolean() && result.asBoolean();
    }

    public String status() {
        if (!enabled) {
            return "DISABLED";
        }
        try {
            connection();
            return "CONNECTED";
        } catch (Exception ex) {
            return "ERROR: " + rootMessage(ex);
        }
    }

    @PreDestroy
    public void close() {
        if (connection != null) {
            connection.close();
        }
    }

    private synchronized FabricConnection connection() {
        if (connection != null) {
            return connection;
        }
        try {
            connection = createConnection();
            return connection;
        } catch (Exception ex) {
            throw new IllegalStateException("无法连接原料供应商 Peer：" + rootMessage(ex), ex);
        }
    }

    private FabricConnection createConnection() throws Exception {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(new FabricUser(
                mspId,
                read(clientCert),
                privateKey(read(clientKey))
        ));

        Peer peer = client.newPeer(
                peerAuthority,
                endpoint,
                tlsProperties(peerTls, peerAuthority)
        );
        Orderer orderer = client.newOrderer(
                ordererAuthority,
                ordererEndpoint,
                tlsProperties(ordererTls, ordererAuthority)
        );
        Channel channel = client.newChannel(channelName);
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
            this.name = "supplier-client";
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
