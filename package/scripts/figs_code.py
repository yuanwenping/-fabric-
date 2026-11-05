# -*- coding: utf-8 -*-
"""第5章实现图与第6章链上存证图"""
import os, sys, re
sys.path.insert(0, os.path.dirname(__file__))
from figlib import Fig, quit_app

ROOT = r'E:\毕业项目实训\pakeage'
BASE = os.path.join(ROOT, 'docs')
VSDX = os.path.join(BASE, '图源文件')
PNG = os.path.join(BASE, '图片')


def extract(rel, marker, count, indent_strip=True):
    path = os.path.join(ROOT, rel)
    lines = open(path, encoding='utf-8').read().splitlines()
    start = None
    for i, ln in enumerate(lines):
        if marker in ln:
            start = i
            break
    if start is None:
        raise SystemExit('marker not found: ' + marker)
    out = lines[start:start + count]
    out = [ln.replace('\t', '    ').rstrip() for ln in out]
    if indent_strip:
        widths = [len(ln) - len(ln.lstrip(' ')) for ln in out if ln.strip()]
        cut = min(widths) if widths else 0
        out = [ln[cut:] if ln.strip() else '' for ln in out]
    return wrap_all(out)


def wrap_all(lines, limit=90):
    out = []
    for ln in lines:
        cur = ln
        while len(cur) > limit:
            cut = cur.rfind(' ', 0, limit)
            if cut <= 0:
                cut = limit
            head = cur[:cut]
            indent = len(cur) - len(cur.lstrip(' '))
            cur = ' ' * (indent + 8) + cur[cut:].lstrip(' ')
            out.append(head)
        out.append(cur)
    return out


def _textbox(f, W, lines, size, pad=0.10):
    box = f.rect(0.10, 0.10, W - 0.20, 0.6, '', fill=True)
    box.Text = chr(10).join(lines)
    box.CellsU('Char.Font').FormulaU = '"Consolas"'
    box.CellsU('Char.AsianFont').FormulaU = '"宋体"'
    box.CellsU('Char.Size').FormulaU = f'{size*3} pt'
    box.CellsU('Para.HorzAlign').FormulaU = '0'
    box.CellsU('VerticalAlign').FormulaU = '0'
    for m in ('LeftMargin', 'RightMargin', 'TopMargin', 'BottomMargin'):
        box.CellsU(m).FormulaU = f'{pad*3} in'
    txt = box.CellsU('TxtHeight').ResultIU / 3.0
    h = txt + pad * 2
    box.CellsU('Height').FormulaU = f'{h*3} in'
    box.CellsU('PinY').FormulaU = f'{(f.h - 0.10)*3} in'
    return h


def code_fig(no, name, lines, W=5.60):
    maxlen = max(len(l) for l in lines)
    size = min(7.6, 640.0 / max(maxlen, 40))
    f = Fig(W, 4.0, f'图{no}')
    h = _textbox(f, W, lines, size)
    stem = f'图{no}-{name}'
    f.save(os.path.join(VSDX, stem + '.vsdx'), os.path.join(PNG, f'fig{no.replace(".", "_")}.png'))
    print('ok', stem, 'maxlen', maxlen, 'size', round(size, 2), 'h', round(h, 2))


# 图5.1 目录与证书结构
def fig_tree():
    rows = [
        'backend/src/main/resources/fabric/',
        '├── org1/                      Org1MSP（供应链企业组织）',
        '│   ├── cert.pem               签名证书',
        '│   ├── key.pem                私钥',
        '│   └── tls-ca.pem             TLS 根证书',
        '├── org2/                      Org2MSP（监管组织）',
        '│   ├── cert.pem',
        '│   ├── key.pem',
        '│   └── tls-ca.pem',
        '└── orderer-tls-ca.pem         Orderer TLS 根证书',
        '',
        'chaincode/tracecc/',
        '├── main.go                    链码入口 contractapi.NewChaincode',
        '├── contract.go                TraceContract 业务方法',
        '└── go.mod / go.sum            依赖：fabric-contract-api-go',
        '',
        'Fabric 网络与通道',
        '├── 通道名称     tracechannel',
        '├── 链码名称     tracecc',
        '├── Org1 Peer    grpcs://<peer0.org1>:7051',
        '├── Org2 Peer    grpcs://<peer0.org2>:9051',
        '└── Orderer      grpcs://<orderer>:7050',
    ]
    maxlen = max(len(r) for r in rows)
    size = min(8.0, 650.0 / maxlen)
    f = Fig(5.65, 4.0, '图6.1')
    _textbox(f, 5.65, rows, size)
    f.save(os.path.join(VSDX, '图6.1-链上存证记录.vsdx'),
           os.path.join(PNG, 'fig6_1.png'))
    print('ok 图6.1')


if __name__ == '__main__':
    fig_tree()
    code_fig('5.2', '链码AppendEvent实现',
             extract(r'chaincode\tracecc\contract.go', 'func (c *TraceContract) AppendEvent', 32))
    code_fig('5.3', '上链载荷与交易提交',
             extract(r'backend\src\main\java\com\changde\trace\trace\TraceService.java',
                     'Map<String, Object> chainPayload = new TreeMap<>();', 30))
    ev = extract(r'backend\src\main\java\com\changde\trace\trace\TraceService.java',
                 'private String computeEvidenceRoot', 12)
    ev2 = extract(r'backend\src\main\java\com\changde\trace\trace\EvidenceService.java',
                  'private String sha256(Path file)', 10)
    code_fig('5.4', '附件摘要与证据根计算', ev + [''] + ev2)
    code_fig('5.5', '交接单与保管方推进',
             extract(r'backend\src\main\java\com\changde\trace\trace\TraceService.java',
                     'private void prepareTransfer', 30))
    code_fig('5.6', '前端链上状态与核验调用',
             extract(r'frontend\src\views\BatchDetailView.vue', 'async function verify()', 18))
    fig_chain_table()
    quit_app()
