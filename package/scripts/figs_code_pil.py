# -*- coding: utf-8 -*-
"""第5章代码清单图、第6章链上存证图（位图渲染，源文件在 docs/裁剪源）"""
import os, sys
from PIL import Image, ImageDraw, ImageFont

ROOT = r'E:\毕业项目实训\pakeage'
PNG = os.path.join(ROOT, 'docs', '图片')
MONO = r'C:\Windows\Fonts\consola.ttf'
CJK = r'C:\Windows\Fonts\simsun.ttc'


def is_cjk(ch):
    return '\u2e80' <= ch <= '\u9fff' or '\uff00' <= ch <= '\uffef'


def render(lines, out, size=20, pad=14, dpi_note=''):
    f_lat = ImageFont.truetype(MONO, size)
    f_cjk = ImageFont.truetype(CJK, size)
    lh = int(size * 1.34)
    # width
    maxw = 0
    for ln in lines:
        w = 0
        for ch in ln:
            f = f_cjk if is_cjk(ch) else f_lat
            w += f.getlength(ch)
        maxw = max(maxw, w)
    W = int(maxw) + pad * 2
    H = int(lh * len(lines)) + pad * 2
    img = Image.new('RGB', (W, H), 'white')
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], outline=(120, 120, 120), width=1)
    y = pad
    for ln in lines:
        x = pad
        for ch in ln:
            f = f_cjk if is_cjk(ch) else f_lat
            d.text((x, y), ch, font=f, fill=(20, 20, 20))
            x += f.getlength(ch)
        y += lh
    img.save(out, dpi=(300, 300))
    print('ok', os.path.basename(out), img.size)


def extract(rel, marker, count):
    path = os.path.join(ROOT, rel)
    raw = open(path, encoding='utf-8').read().splitlines()
    start = next(i for i, l in enumerate(raw) if marker in l)
    out = [l.replace('\t', '    ').rstrip() for l in raw[start:start + count]]
    widths = [len(l) - len(l.lstrip(' ')) for l in out if l.strip()]
    cut = min(widths) if widths else 0
    out = [l[cut:] if l.strip() else '' for l in out]
    return wrap(out)


def wrap(lines, limit=92):
    res = []
    for ln in lines:
        cur = ln
        while len(cur) > limit:
            cut = cur.rfind(' ', 0, limit)
            cut = cut if cut > 0 else limit
            indent = len(cur) - len(cur.lstrip(' '))
            res.append(cur[:cut])
            cur = ' ' * (indent + 8) + cur[cut:].lstrip(' ')
        res.append(cur)
    return res


TREE = [
    'backend/src/main/resources/fabric/                Fabric 身份材料',
    '├── org1/cert.pem        Org1MSP 用户签名证书（供应链企业）',
    '├── org1/key.pem         与证书匹配的私钥',
    '├── org1/tls-ca.pem      Peer TLS 根证书',
    '├── org2/cert.pem        Org2MSP 用户签名证书（监管部门）',
    '├── org2/key.pem',
    '├── org2/tls-ca.pem',
    '└── orderer-tls-ca.pem   Orderer TLS 根证书',
    '',
    'chaincode/tracecc/                                链码工程',
    '├── main.go              contractapi.NewChaincode 链码入口',
    '├── contract.go          TraceContract 业务方法集合',
    '└── go.mod               fabric-contract-api-go 依赖',
    '',
    'application.yml                                    后端链连接参数',
    '├── fabric.channel       tracechannel',
    '├── fabric.chaincode     tracecc',
    '├── fabric.msp-org1      Org1MSP',
    '└── fabric.msp-org2      Org2MSP',
]

CHAIN_ROWS = [
    'mysql> SELECT event_type, stage, chain_status, LEFT(tx_id,16) tx, block_number',
    '    -> FROM trace_event ORDER BY block_number;',
    '+--------------+-------------+-------------+------------------+--------------+',
    '| event_type   | stage       | chain_status| tx               | block_number |',
    '+--------------+-------------+-------------+------------------+--------------+',
    '| QUARANTINE   | QUARANTINED | COMMITTED   | 37af2b8f4d505565 |           10 |',
    '| DISPATCH     | DISPATCHED  | COMMITTED   | 27c30a2c0b100820 |           11 |',
    '| RECEIVE      | RECEIVED    | COMMITTED   | 931e92c7176f26a2 |           12 |',
    '| SLAUGHTER    | SLAUGHTERED | COMMITTED   | 4062102be3ed7e2b |           14 |',
    '| MARINATE     | MARINATED   | COMMITTED   | 92a1b4eb1c5ced9c |           15 |',
    '| SAUCE        | SAUCED      | COMMITTED   | c22fef0cd8daa0b9 |           16 |',
    '| ROAST        | ROASTED     | COMMITTED   | 94f088fc4004f95e |           17 |',
    '| PACKAGE      | PACKAGED    | COMMITTED   | 0b1c5898cb518109 |           18 |',
    '| WAREHOUSE_IN | WAREHOUSED  | COMMITTED   | a9f1b2e9ef2e118f |           19 |',
    '| DISPATCH     | DISPATCHED  | COMMITTED   | 8682a681899aa48b |           20 |',
    '+--------------+-------------+-------------+------------------+--------------+',
    '10 rows in set (0.01 sec)',
]

if __name__ == '__main__':
    render(TREE, os.path.join(PNG, 'fig5_1.png'), size=20)
    render(extract(r'chaincode\tracecc\contract.go',
                   'func (c *TraceContract) AppendEvent', 30),
           os.path.join(PNG, 'fig5_2.png'), size=20)
    render(extract(r'backend\src\main\java\com\changde\trace\trace\TraceService.java',
                   'Map<String, Object> chainPayload = new TreeMap<>();', 28),
           os.path.join(PNG, 'fig5_3.png'), size=20)
    ev = extract(r'backend\src\main\java\com\changde\trace\trace\TraceService.java',
                 'private String computeEvidenceRoot', 12)
    ev2 = extract(r'backend\src\main\java\com\changde\trace\trace\EvidenceService.java',
                  'private String sha256(Path file)', 9)
    render(ev + [''] + ev2, os.path.join(PNG, 'fig5_4.png'), size=20)
    render(extract(r'backend\src\main\java\com\changde\trace\trace\TraceService.java',
                   'private void prepareTransfer', 28),
           os.path.join(PNG, 'fig5_5.png'), size=20)
    render(extract(r'frontend\src\views\BatchDetailView.vue', 'async function verify()', 16),
           os.path.join(PNG, 'fig5_6.png'), size=20)
    render(CHAIN_ROWS, os.path.join(PNG, 'fig6_1.png'), size=20)
