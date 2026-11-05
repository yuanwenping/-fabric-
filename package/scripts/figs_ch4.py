# -*- coding: utf-8 -*-
"""Ch4 系统设计图：总体架构、功能图、九个流程图、Fabric网络、区块结构、E-R"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from figlib import Fig, quit_app, BLACK

BASE = r'E:\毕业项目实训\pakeage\docs'
VSDX = os.path.join(BASE, '图源文件')
PNG = os.path.join(BASE, '图片')
os.makedirs(VSDX, exist_ok=True)
os.makedirs(PNG, exist_ok=True)


def save(f, no, name):
    stem = f'图{no}-{name}'
    f.save(os.path.join(VSDX, stem + '.vsdx'),
           os.path.join(PNG, f'fig{no.replace(".", "_")}.png'))
    print('ok', stem)


# ---------------- flowchart helpers ----------------
def node(f, kind, cx, y, text, w=1.75, h=0.44, size=9.5):
    if kind == 'dec':
        w, h = 2.35, 0.66
    x = cx - w / 2.0
    if kind in ('start', 'end'):
        f.rrect(x, y, w, h, text, r=0.14, size=size)
    elif kind == 'dec':
        f.diamond(x, y, w, h, text, size=size)
    else:
        f.rect(x, y, w, h, text, size=size)
    return dict(x=x, y=y, w=w, h=h, cx=cx)


def down(f, a, b, label=None, lx=0.0, ly=0.0):
    f.arrow(a['cx'], a['y'] + a['h'], b['cx'], b['y'])
    if label:
        f.label(a['cx'] + 0.05 + lx, (a['y'] + a['h'] + b['y']) / 2 - 0.13, 0.4, 0.24, label, size=9.5)


def branch(f, a, b, label, side='right', label_dy=-0.22):
    """right/left branch: from a's side to b's top or side."""
    if side == 'right':
        f.path([(a['x'] + a['w'], a['y'] + a['h'] / 2), (b['cx'], a['y'] + a['h'] / 2),
                (b['cx'], b['y'])])
        f.label(a['x'] + a['w'] + 0.06, a['y'] + a['h'] / 2 + label_dy, 0.5, 0.22, label, size=9.5)
    else:
        f.path([(a['x'], a['y'] + a['h'] / 2), (b['cx'], a['y'] + a['h'] / 2),
                (b['cx'], b['y'])])
        f.label(a['x'] - 0.56, a['y'] + a['h'] / 2 + label_dy, 0.5, 0.22, label, size=9.5)


def merge_to(f, a, b, side='right'):
    """from a bottom -> route sideways -> into b top"""
    if side == 'right':
        f.path([(a['cx'], a['y'] + a['h']), (a['cx'], a['y'] + a['h'] + 0.16),
                (b['cx'], a['y'] + a['h'] + 0.16), (b['cx'], b['y'])])
    else:
        f.path([(a['cx'], a['y'] + a['h']), (a['cx'], a['y'] + a['h'] + 0.16),
                (b['cx'], a['y'] + a['h'] + 0.16), (b['cx'], b['y'])])


def loop_back(f, a, target, side='right', label='否'):
    """a bottom -> out to side -> up -> into target side"""
    if side == 'right':
        rx = max(a['x'] + a['w'], target['x'] + target['w']) + 0.30
        my = a['y'] + a['h'] / 2
        f.path([(a['x'] + a['w'], my), (rx, my), (rx, target['y'] + target['h'] / 2),
                (target['x'] + target['w'], target['y'] + target['h'] / 2)])
        f.label(a['x'] + a['w'] + 0.04, my - 0.24, 0.42, 0.22, label, size=9.5)
    else:
        lx = min(a['x'], target['x']) - 0.30
        my = a['y'] + a['h'] / 2
        f.path([(a['x'], my), (lx, my), (lx, target['y'] + target['h'] / 2),
                (target['x'], target['y'] + target['h'] / 2)])
        f.label(lx + 0.03, my - 0.24, 0.42, 0.22, label, size=9.5)


# =============== 图4.1 系统总体架构 ===============
def fig_arch():
    W, H = 5.7, 3.55
    f = Fig(W, H, '图4.1')
    f.label(0.10, 0.02, 5.5, 0.28, '常德酱板鸭全链条可信溯源系统', size=11.5, bold=True)
    f.rect(0.12, 0.34, 5.46, 2.62, '', fill=False)
    rows = [
        ('展示层', 0.42, 0.50,
         ['企业工作台', '批次管理页', '业务填报页', '审核上链页', '平台管理页', '公众查询页']),
        ('业务层', 1.00, 0.88, None),
        ('接口层', 1.94, 0.46,
         ['REST 接口', 'JWT 鉴权', 'Fabric Java SDK', '附件存储']),
        ('数据层', 2.48, 0.42, None),
    ]
    for name, y, h, items in rows:
        f.rect(0.20, y, 0.30, h, name, size=8)
        if items:
            n = len(items)
            gap = 0.07
            bw = (4.98 - gap * (n - 1)) / n
            for i, t in enumerate(items):
                f.rect(0.58 + i * (bw + gap), y + 0.05, bw, h - 0.10, t, size=9)
    items = [['账号与组织服务', '批次与流转服务', '岗位字段校验'],
             ['审核与上链服务', '监管处置服务', '查询与核验服务']]
    bw, bh = 1.58, 0.36
    for r, rowitems in enumerate(items):
        for c, t in enumerate(rowitems):
            f.rect(0.58 + c * (bw + 0.12), 1.06 + r * (bh + 0.06), bw, bh, t, size=9)
    f.rect(0.58, 2.54, 2.30, 0.28, 'MySQL 8.0 业务数据库', size=9)
    f.rect(3.00, 2.54, 2.48, 0.28, 'Hyperledger Fabric 联盟链', size=9)
    for y in (0.97, 1.91, 2.44):
        f.path([(0.16, y), (5.54, y)], arrow=False, dash=True)
    f.rect(0.55, 3.10, 2.40, 0.34, '链下业务明细与附件文件', size=9)
    f.rect(3.10, 3.10, 2.40, 0.34, 'tracechannel 通道 / tracecc 链码', size=9)
    f.path([(1.75, 3.10), (1.75, 2.84)], arrow=True, both=True)
    f.path([(4.30, 3.10), (4.30, 2.84)], arrow=True, both=True)
    save(f, '4.1', '系统总体架构图')


# =============== 图4.2 系统总体功能 ===============
def fig_func_tree():
    W, H = 5.75, 2.80
    f = Fig(W, H, '图4.2')
    f.rect(1.35, 0.10, 3.0, 0.44, '常德酱板鸭全链条可信溯源系统', size=10.5)
    groups = [
        ('账号与组织', ['企业注册', '员工账号', '权限管理']),
        ('批次管理', ['原料批次', '加工批次', '批次关系']),
        ('业务填报', ['岗位表单', '附件证据', '提交审核']),
        ('审核与上链', ['审核意见', '链码提交', '失败重试']),
        ('交接流转', ['发货交接', '运输在途', '收货确认']),
        ('监管与查询', ['监管处置', '追溯查询', '存证核验']),
    ]
    gx0 = 0.18
    gw = 0.86          # group pitch
    bw = 0.44          # level-2 box width
    ly2 = 0.62
    ly3 = 1.42
    lh3 = 1.12
    # horizontal bus
    bus_y = 0.56
    f.path([(2.85, 0.54), (2.85, bus_y)], arrow=False)
    first_cx = gx0 + bw / 2.0
    last_cx = gx0 + 5 * gw + bw / 2.0
    f.path([(first_cx, bus_y), (last_cx, bus_y)], arrow=False)
    for i, (g, leaves) in enumerate(groups):
        cx = gx0 + i * gw + bw / 2.0
        f.path([(cx, bus_y), (cx, ly2)], arrow=False)
        f.rect(gx0 + i * gw, ly2, bw, 0.72, g, size=8.5, textdir=1)
        # leaves
        n = len(leaves)
        lw = 0.26
        gap = 0.03
        total = n * lw + (n - 1) * gap
        startx = cx - total / 2.0
        # connector
        f.path([(cx, ly2 + 0.72), (cx, ly3 - 0.12)], arrow=False)
        f.path([(startx + lw / 2.0, ly3 - 0.12), (startx + total - lw / 2.0, ly3 - 0.12)], arrow=False)
        for j, lf in enumerate(leaves):
            lx = startx + j * (lw + gap)
            f.path([(lx + lw / 2.0, ly3 - 0.12), (lx + lw / 2.0, ly3)], arrow=False)
            f.rect(lx, ly3, lw, lh3, lf, size=8, textdir=1)
    save(f, '4.2', '系统总体功能图')


# =============== 图4.3 注册登录流程图 ===============
def fig_flow_login():
    W, H = 5.0, 6.75
    f = Fig(W, H, '图4.3', ox=0.35)
    cx = 1.70
    n1 = node(f, 'start', cx, 0.10, '开始')
    d0 = node(f, 'dec', cx, 0.66, '是否首次使用本平台')
    r1 = node(f, 'proc', cx, 1.52, '提交企业注册申请', w=2.0)
    r2 = node(f, 'proc', cx, 2.04, '平台审核企业资质', w=2.0)
    d1 = node(f, 'dec', cx, 2.56, '审核是否通过')
    r3 = node(f, 'proc', 3.95, 2.90, '驳回并通知修改', w=1.45)
    r4 = node(f, 'proc', cx, 3.52, '生成账号与岗位角色', w=2.1)
    l1 = node(f, 'proc', cx, 4.04, '输入手机号与密码', w=2.0)
    d2 = node(f, 'dec', cx, 4.56, '凭证正确且账号启用')
    l2 = node(f, 'proc', 3.95, 4.90, '提示错误并重新登录', w=1.45)
    l3 = node(f, 'proc', cx, 5.52, '加载岗位菜单与数据范围', w=2.3)
    e = node(f, 'end', cx, 6.04, '结束')
    down(f, n1, d0)
    # 否：已有账号，左侧旁路进入登录
    f.path([(d0['x'], d0['y'] + d0['h'] / 2), (0.22, d0['y'] + d0['h'] / 2),
            (0.22, l1['y'] + 0.22), (l1['x'], l1['y'] + 0.22)])
    f.tag(0.26, d0['y'] + d0['h'] / 2 - 0.30, 0.30, 0.22, '否', size=9.5)
    down(f, d0, r1, '是', lx=0.08)
    down(f, r1, r2); down(f, r2, d1)
    branch(f, d1, r3, '否', 'right')
    down(f, d1, r4, '是', lx=0.10)
    down(f, r4, l1); down(f, l1, d2)
    branch(f, d2, l2, '否', 'right')
    down(f, d2, l3, '是', lx=0.10)
    down(f, l3, e)
    # 驳回与登录失败均汇入结束
    f.path([(r3['x'] + r3['w'], r3['y'] + r3['h'] / 2), (r3['x'] + r3['w'] + 0.22, r3['y'] + r3['h'] / 2),
            (r3['x'] + r3['w'] + 0.22, e['y'] + 0.22), (e['x'] + e['w'], e['y'] + 0.22)])
    f.path([(l2['x'] + l2['w'] / 2, l2['y'] + l2['h']), (l2['x'] + l2['w'] / 2, e['y'] + 0.22),
            (e['x'] + e['w'], e['y'] + 0.22)])
    save(f, '4.3', '注册登录流程图')


# =============== 图4.4 批次创建流程图 ===============
def fig_flow_batch():
    W, H = 4.9, 4.9
    f = Fig(W + 0.7, H, '图4.4', ox=0.35)
    cx = 1.85
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.66, '选择批次类型', w=1.85)
    n3 = node(f, 'dec', cx, 1.24, '是否为加工批次')
    n4 = node(f, 'proc', 3.70, 1.55, '关联上游批次', w=1.4)
    n5 = node(f, 'proc', cx, 2.10, '填写产品与数量信息', w=1.85)
    n6 = node(f, 'dec', cx, 2.68, '必填与阶段是否合规')
    n7 = node(f, 'proc', 3.70, 3.05, '返回修改并提示', w=1.4)
    n8 = node(f, 'proc', cx, 3.55, '生成批次号并入库', w=1.85)
    n9 = node(f, 'proc', cx, 4.10, '调用CreateBatch上链', w=1.85)
    n10 = node(f, 'dec', cx, 4.62, '上链是否成功')
    n11 = node(f, 'proc', 3.70, 5.00, '标记CHAIN_FAILED', w=1.45)
    n12 = node(f, 'proc', cx, 5.50, '批次状态置COMMITTED', w=1.85)
    n13 = node(f, 'end', cx, 6.05, '结束')
    H = 6.65
    f.pg.PageSheet.CellsU('PageHeight').FormulaU = f'{H*3} in'
    f.h = H
    # re-draw? page height change breaks Y mapping -> rebuild
    return None


def fig_flow_batch2():
    W, H = 4.95, 6.6
    f = Fig(W + 0.7, H, '图4.4', ox=0.35)
    cx = 1.85
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.66, '选择批次类型', w=1.85)
    n3 = node(f, 'dec', cx, 1.24, '是否为加工批次')
    n4 = node(f, 'proc', 3.75, 1.55, '关联上游批次', w=1.4)
    n5 = node(f, 'proc', cx, 2.10, '填写产品与数量信息', w=1.85)
    n6 = node(f, 'dec', cx, 2.68, '必填与阶段是否合规')
    n7 = node(f, 'proc', 3.75, 3.02, '返回修改并提示', w=1.4)
    n8 = node(f, 'proc', cx, 3.55, '生成批次号并入库', w=1.85)
    n9 = node(f, 'proc', cx, 4.10, '调用CreateBatch上链', w=1.85)
    n10 = node(f, 'dec', cx, 4.68, '上链是否成功')
    n11 = node(f, 'proc', 3.75, 5.02, '标记CHAIN_FAILED', w=1.45)
    n12 = node(f, 'proc', cx, 5.60, '批次状态置COMMITTED', w=1.85)
    n13 = node(f, 'end', cx, 6.15, '结束')
    down(f, n1, n2); down(f, n2, n3)
    branch(f, n3, n4, '是', 'right')
    f.path([(n4['cx'], n4['y'] + n4['h']), (n4['cx'], n4['y'] + n4['h'] + 0.12),
            (n5['cx'], n4['y'] + n4['h'] + 0.12), (n5['cx'], n5['y'])])
    f.label(n3['cx'] + 0.10, n3['y'] + n3['h'] + 0.02, 0.3, 0.22, '否', size=9.5)
    down(f, n3, n5)
    down(f, n5, n6)
    branch(f, n6, n7, '否', 'right')
    loop_back(f, n7, n5, 'right', '')
    down(f, n6, n8, '是', lx=0.22)
    down(f, n8, n9); down(f, n9, n10)
    branch(f, n10, n11, '否', 'right')
    down(f, n10, n12, '是', lx=0.22)
    down(f, n12, n13)
    save(f, '4.4', '批次创建流程图')


# =============== 图4.5 业务填报流程图 ===============
def fig_flow_fill():
    W, H = 4.95, 5.6
    f = Fig(W + 0.7, H, '图4.5', ox=0.35)
    cx = 1.85
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.66, '加载组织与岗位', w=1.9)
    n3 = node(f, 'proc', cx, 1.18, '获取允许业务类型', w=1.9)
    n4 = node(f, 'dec', cx, 1.74, '业务类型是否合法')
    n5 = node(f, 'proc', 3.80, 2.02, '拒绝提交并返回403', w=1.45)
    n6 = node(f, 'proc', cx, 2.58, '填写岗位白名单字段', w=1.9)
    n7 = node(f, 'proc', cx, 3.10, '上传附件并计算摘要', w=1.9)
    n8 = node(f, 'dec', cx, 3.66, '必填与格式是否合规')
    n9 = node(f, 'proc', 3.80, 3.94, '提示缺失字段', w=1.45)
    n10 = node(f, 'proc', cx, 4.50, '保存事件待审核', w=1.9)
    n11 = node(f, 'end', cx, 5.05, '结束')
    down(f, n1, n2); down(f, n2, n3); down(f, n3, n4)
    branch(f, n4, n5, '否', 'right')
    down(f, n4, n6, '是', lx=0.22)
    down(f, n6, n7); down(f, n7, n8)
    branch(f, n8, n9, '否', 'right')
    loop_back(f, n9, n6, 'right', '')
    down(f, n8, n10, '是', lx=0.22)
    down(f, n10, n11)
    save(f, '4.5', '岗位业务填报流程图')


# =============== 图4.6 审核与上链流程图 ===============
def fig_flow_review():
    W, H = 5.1, 7.65
    f = Fig(W + 0.7, H, '图4.6', ox=0.35)
    cx = 1.85
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.64, '打开待审核列表', w=1.9)
    n3 = node(f, 'proc', cx, 1.16, '查看填报与附件', w=1.9)
    n4 = node(f, 'dec', cx, 1.72, '审核是否通过')
    n5 = node(f, 'proc', 3.85, 2.00, '填写驳回意见', w=1.45)
    n6 = node(f, 'proc', 3.85, 2.52, '状态置REJECTED', w=1.45)
    n7 = node(f, 'proc', cx, 3.05, '组装规范化载荷', w=1.9)
    n8 = node(f, 'proc', cx, 3.57, '计算内容哈希与证据根', w=2.0)
    n9 = node(f, 'proc', cx, 4.09, '调用AppendEvent提交', w=1.9)
    n10 = node(f, 'dec', cx, 4.65, '上链是否成功')
    n11 = node(f, 'proc', 3.85, 4.93, '标记CHAIN_FAILED', w=1.45)
    n12 = node(f, 'proc', cx, 5.55, '写回TxID与区块高度', w=2.0)
    n13 = node(f, 'proc', cx, 6.07, '推进阶段与保管方', w=1.9)
    n14 = node(f, 'proc', cx, 6.59, '生成下游交接任务', w=1.9)
    n15 = node(f, 'end', cx, 7.11, '结束')
    down(f, n1, n2); down(f, n2, n3); down(f, n3, n4)
    branch(f, n4, n5, '否', 'right')
    f.label(n4['x'] + 0.12, n4['y'] + n4['h'] + 0.02, 0.3, 0.22, '是', size=9.5)
    down(f, n5, n6)
    down(f, n4, n7)
    down(f, n7, n8); down(f, n8, n9); down(f, n9, n10)
    branch(f, n10, n11, '否', 'right')
    down(f, n10, n12, '是', lx=0.22)
    down(f, n12, n13); down(f, n13, n14); down(f, n14, n15)
    save(f, '4.6', '审核上链流程图')


# =============== 图4.7 交接流转流程图 ===============
def fig_flow_transfer():
    W, H = 5.0, 6.7
    f = Fig(W + 0.7, H, '图4.7', ox=0.35)
    cx = 1.9
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.64, '提交发货登记', w=1.95)
    n3 = node(f, 'proc', cx, 1.16, '审核通过并上链', w=1.95)
    n4 = node(f, 'proc', cx, 1.68, '生成待交接单据', w=1.95)
    n5 = node(f, 'proc', cx, 2.20, '物流登记运输信息', w=1.95)
    n6 = node(f, 'dec', cx, 2.76, '冷链温控是否合规')
    n7 = node(f, 'proc', 3.75, 3.04, '记录温控异常', w=1.4)
    n8 = node(f, 'proc', 3.75, 3.56, '推送监管处置', w=1.4)
    n9 = node(f, 'proc', cx, 4.12, '下游企业签收', w=1.95)
    n10 = node(f, 'dec', cx, 4.68, '数量与包装是否一致')
    n11 = node(f, 'proc', 3.75, 4.96, '记录差异码并挂起', w=1.5)
    n12 = node(f, 'proc', cx, 5.62, '变更保管方与阶段', w=1.95)
    n13 = node(f, 'end', cx, 6.14, '结束')
    down(f, n1, n2); down(f, n2, n3); down(f, n3, n4); down(f, n4, n5); down(f, n5, n6)
    branch(f, n6, n7, '否', 'right')
    down(f, n7, n8)
    f.label(n6['x'] + 0.12, n6['y'] + n6['h'] + 0.02, 0.3, 0.22, '是', size=9.5)
    down(f, n6, n9)
    f.path([(n8['cx'], n8['y'] + n8['h']), (n8['cx'], n9['y'] + 0.12), (n9['x'] + n9['w'], n9['y'] + 0.12)])
    down(f, n9, n10)
    branch(f, n10, n11, '否', 'right')
    down(f, n10, n12, '是', lx=0.22)
    down(f, n12, n13)
    save(f, '4.7', '交接流转流程图')


# =============== 图4.8 监管处置流程图 ===============
def fig_flow_reg():
    W, H = 5.0, 6.6
    f = Fig(W + 0.7, H, '图4.8', ox=0.35)
    cx = 1.85
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.64, '选择批次与监管业务', w=2.0)
    n3 = node(f, 'dec', cx, 1.20, '是否为监督抽检')
    n4 = node(f, 'proc', 3.80, 1.48, '录入抽检指标', w=1.4)
    n5 = node(f, 'proc', 3.80, 2.00, '生成检验结论', w=1.4)
    n6 = node(f, 'proc', cx, 2.58, '填写处置原因与范围', w=2.0)
    n7 = node(f, 'dec', cx, 3.14, '监管身份是否有效')
    n8 = node(f, 'proc', 3.80, 3.42, '拒绝写入', w=1.3)
    n9 = node(f, 'proc', cx, 3.98, '提交监管事件待审核', w=2.0)
    n10 = node(f, 'proc', cx, 4.50, '审核通过写入Fabric', w=2.0)
    n11 = node(f, 'proc', cx, 5.02, '更新冻结或召回状态', w=2.0)
    n12 = node(f, 'proc', cx, 5.54, '通知相关企业', w=1.8)
    n13 = node(f, 'end', cx, 6.06, '结束')
    down(f, n1, n2); down(f, n2, n3)
    branch(f, n3, n4, '是', 'right')
    down(f, n4, n5)
    f.label(n3['x'] + 0.12, n3['y'] + n3['h'] + 0.02, 0.3, 0.22, '否', size=9.5)
    down(f, n3, n6)
    f.path([(n5['cx'], n5['y'] + n5['h']), (n5['cx'], n6['y'] + 0.14), (n6['x'] + n6['w'], n6['y'] + 0.14)])
    down(f, n6, n7)
    branch(f, n7, n8, '否', 'right')
    down(f, n7, n9, '是', lx=0.22)
    down(f, n9, n10); down(f, n10, n11); down(f, n11, n12); down(f, n12, n13)
    save(f, '4.8', '监管处置流程图')


# =============== 图4.9 溯源查询与核验流程图 ===============
def fig_flow_query():
    W, H = 5.1, 6.8
    f = Fig(W + 0.7, H, '图4.9', ox=0.35)
    cx = 1.9
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.64, '输入追溯码或批次号', w=2.0)
    n3 = node(f, 'dec', cx, 1.20, '记录是否存在')
    n4 = node(f, 'proc', 3.85, 1.48, '提示未查询到', w=1.35)
    n5 = node(f, 'proc', cx, 2.02, '读取批次与事件数据', w=2.0)
    n6 = node(f, 'proc', cx, 2.54, '按角色过滤公开字段', w=2.0)
    n7 = node(f, 'proc', cx, 3.06, '时间线展示产品履历', w=2.0)
    n8 = node(f, 'dec', cx, 3.62, '是否发起存证核验')
    n9 = node(f, 'proc', cx, 4.24, '逐事件调用VerifyEvent', w=2.05)
    n10 = node(f, 'dec', cx, 4.80, '哈希是否全部一致')
    n11 = node(f, 'proc', 3.85, 5.08, '定位异常事件', w=1.4)
    n12 = node(f, 'proc', cx, 5.68, '显示已上链且一致', w=2.0)
    n13 = node(f, 'end', cx, 6.20, '结束')
    down(f, n1, n2); down(f, n2, n3)
    branch(f, n3, n4, '否', 'right')
    down(f, n3, n5, '是', lx=0.22)
    down(f, n5, n6); down(f, n6, n7); down(f, n7, n8)
    down(f, n8, n9, '是', lx=0.22)
    down(f, n9, n10)
    branch(f, n10, n11, '否', 'right')
    down(f, n10, n12, '是', lx=0.22)
    down(f, n12, n13)
    # 否 to end
    f.path([(n8['cx'] + 0.0, n8['y'] + n8['h']), (n8['cx'], n8['y'] + n8['h'] + 0.10),
            (4.65, n8['y'] + n8['h'] + 0.10), (4.65, n13['y'] + n13['h'] / 2),
            (n13['x'] + n13['w'], n13['y'] + n13['h'] / 2)])
    f.label(n8['x'] + n8['w'] + 0.02, n8['y'] + n8['h'] + 0.12, 0.3, 0.22, '否', size=9.5)
    save(f, '4.9', '溯源查询与核验流程图')


# =============== 图4.10 组织与账号管理流程图 ===============
def fig_flow_admin():
    W, H = 4.9, 6.15
    f = Fig(W + 0.7, H, '图4.10', ox=0.35)
    cx = 1.8
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.64, '平台管理员登录', w=1.9)
    n3 = node(f, 'proc', cx, 1.16, '查看待审核企业', w=1.9)
    n4 = node(f, 'dec', cx, 1.72, '资料是否合规')
    n5 = node(f, 'proc', 3.75, 2.00, '驳回并记录原因', w=1.45)
    n6 = node(f, 'proc', cx, 2.56, '创建组织并绑定MSP', w=2.0)
    n7 = node(f, 'proc', cx, 3.08, '生成企业主账号', w=1.9)
    n8 = node(f, 'proc', cx, 3.60, '创建员工与分配岗位', w=2.0)
    n9 = node(f, 'dec', cx, 4.16, '岗位与组织是否匹配')
    n10 = node(f, 'proc', 3.75, 4.44, '提示重新分配', w=1.4)
    n11 = node(f, 'proc', cx, 5.04, '启用账号并写审计日志', w=2.1)
    n12 = node(f, 'end', cx, 5.56, '结束')
    down(f, n1, n2); down(f, n2, n3); down(f, n3, n4)
    branch(f, n4, n5, '否', 'right')
    down(f, n4, n6, '是', lx=0.22)
    down(f, n6, n7); down(f, n7, n8); down(f, n8, n9)
    branch(f, n9, n10, '否', 'right')
    loop_back(f, n10, n8, 'right', '')
    down(f, n9, n11, '是', lx=0.22)
    down(f, n11, n12)
    save(f, '4.10', '组织与账号管理流程图')


# =============== 图4.11 消费者查询流程图 ===============
def fig_flow_consumer():
    W, H = 4.9, 5.6
    f = Fig(W + 0.7, H, '图4.11', ox=0.35)
    cx = 1.8
    n1 = node(f, 'start', cx, 0.10, '开始')
    n2 = node(f, 'proc', cx, 0.64, '打开公众查询页', w=1.85)
    n3 = node(f, 'proc', cx, 1.16, '输入包装追溯码', w=1.85)
    n4 = node(f, 'dec', cx, 1.72, '追溯码是否有效')
    n5 = node(f, 'proc', 3.7, 2.00, '提示无效追溯码', w=1.4)
    n6 = node(f, 'proc', cx, 2.56, '返回公开溯源字段', w=1.95)
    n7 = node(f, 'proc', cx, 3.08, '展示履历与检验结论', w=2.05)
    n8 = node(f, 'dec', cx, 3.64, '是否登录消费者')
    n9 = node(f, 'proc', 3.7, 3.92, '匿名查看后结束', w=1.4)
    n10 = node(f, 'proc', cx, 4.52, '记录历史并支持收藏', w=2.05)
    n11 = node(f, 'end', cx, 5.04, '结束')
    down(f, n1, n2); down(f, n2, n3); down(f, n3, n4)
    branch(f, n4, n5, '否', 'right')
    down(f, n4, n6, '是', lx=0.22)
    down(f, n6, n7); down(f, n7, n8)
    branch(f, n8, n9, '否', 'right')
    down(f, n8, n10, '是', lx=0.22)
    down(f, n10, n11)
    save(f, '4.11', '消费者查询流程图')


# =============== 图4.12 Fabric网络拓扑 ===============
def fig_network():
    W, H = 5.70, 2.95
    f = Fig(W, H, '图4.12')
    f.rect(1.40, 0.62, 3.10, 2.24, '', fill=False)
    f.tag(1.44, 0.66, 0.80, 0.44, 'tracechannel\n业务通道', size=8)
    f.rect(0.05, 1.12, 1.05, 0.62, 'Spring Boot\n后端服务', size=8.5)
    f.rect(1.55, 1.12, 1.30, 0.62, 'Peer0.Org1\nOrg1MSP', size=8.5)
    f.rect(3.00, 1.12, 1.30, 0.62, 'Peer0.Org2\nOrg2MSP', size=8.5)
    f.rect(1.52, 1.90, 2.72, 0.38, '账本副本（批次与事件哈希链）', size=8.5)
    f.rect(1.52, 2.34, 2.72, 0.38, 'tracecc 链码实例', size=8.5)
    f.rect(2.05, 0.10, 1.70, 0.36, 'Orderer 排序服务', size=8.5)
    f.rect(4.62, 1.12, 1.00, 0.62, 'Fabric CA\n证书体系', size=8.5)
    f.path([(1.10, 1.43), (1.55, 1.43)], arrow=True, both=True)
    f.path([(2.20, 1.12), (2.20, 0.46)], arrow=True, both=True)
    f.tag(2.24, 0.76, 0.38, 0.22, '排序', size=8)
    f.path([(3.65, 1.12), (3.65, 0.46)], arrow=True, both=True)
    f.path([(4.62, 1.43), (4.30, 1.43)], arrow=True, both=True, dash=True)
    f.path([(2.20, 1.74), (2.20, 1.90)], arrow=True)
    f.path([(3.65, 1.74), (3.65, 1.90)], arrow=True)
    save(f, '4.12', 'Fabric网络拓扑图')


# =============== 图4.13 区块与事件哈希链 ===============
def fig_block():
    W, H = 5.6, 3.6
    f = Fig(W, H, '图4.13')
    # 前序区块
    f.rect(0.10, 0.55, 0.95, 0.55, '区块#n-1', size=9)
    # 当前区块
    f.rect(1.55, 0.28, 2.55, 2.05, '', fill=False)
    f.tag(2.20, 0.20, 0.75, 0.24, '区块#n', size=9)
    # 区块头
    f.rect(1.68, 0.60, 2.30, 0.72, '', fill=False)
    f.tag(1.72, 0.50, 0.70, 0.22, '区块头', size=8.5, align=0)
    hdr = [('父区块哈希', 1.74, 0.84), ('数据哈希', 2.72, 0.84),
           ('区块高度', 1.74, 1.06), ('时间戳', 2.72, 1.06)]
    for t, x, y in hdr:
        f.rect(x, y, 0.90, 0.19, t, size=8)
    # 区块体
    f.rect(1.68, 1.40, 2.30, 0.82, '', fill=False)
    f.tag(1.72, 1.30, 0.70, 0.22, '区块体', size=8.5, align=0)
    for i, t in enumerate(['交易1\n事件摘要', '交易2\n事件摘要', '交易3\n事件摘要']):
        f.rect(1.74 + i * 0.76, 1.62, 0.72, 0.56, t, size=7.5)
    # 后序区块
    f.rect(4.55, 0.55, 0.95, 0.55, '区块#n+1', size=9)
    f.path([(1.05, 0.82), (1.55, 0.82)], arrow=True)
    f.label(1.02, 0.55, 0.55, 0.22, '父哈希', size=8)
    f.path([(4.10, 0.82), (4.55, 0.82)], arrow=True)
    f.label(4.05, 0.55, 0.55, 0.22, '父哈希', size=8)
    # 事件链
    f.label(0.10, 2.05, 2.4, 0.24, '事件哈希链（同一批次）', size=9, align=0)
    xs = [0.20, 1.45, 2.70, 3.95]
    names = ['事件E1', '事件E2', '事件E3', '事件E4']
    for i, (x, nm) in enumerate(zip(xs, names)):
        f.rect(x, 2.42, 0.85, 0.78, '', fill=False)
        f.label(x, 2.44, 0.85, 0.2, nm, size=8.5)
        f.label(x + 0.03, 2.64, 0.79, 0.54, '内容哈希\n前序哈希', size=7.5)
        if i:
            f.path([(x - 0.18, 2.81), (x, 2.81)], arrow=True)
    f.label(0.12, 3.24, 2.6, 0.22, 'prevHash 指向上一事件内容哈希', size=8, align=0)
    save(f, '4.13', '区块结构与事件哈希链图')


# =============== E-R 实体图 ===============
def er_entity(no, name, attrs, fname):
    n = len(attrs)
    W, H = 4.2, 2.5
    f = Fig(W, H, 'er')
    cx, cy = W / 2.0, 0.62
    f.rect(cx - 0.62, 0.32, 1.24, 0.52, name, size=10.5)
    # attrs around bottom arc
    step = (W - 0.7) / max(n - 1, 1)
    for i, a in enumerate(attrs):
        x = 0.35 + i * step
        y = 1.55 if i % 2 == 0 else 1.62
        f.ellipse(x - 0.31, y, 0.62, 0.36, a, size=8)
        f.path([(cx, 0.84), (x, y)], arrow=False)
    save(f, no, fname)


def fig_er():
    er_entity('4.14', '组织', ['组织编号', '企业名称', '企业类型', 'MSP标识'], '组织实体E-R图')
    er_entity('4.15', '用户', ['用户编号', '登录手机', '所属组织', '岗位角色'], '用户实体E-R图')
    er_entity('4.16', '批次', ['批次编号', '产品名称', '当前阶段', '链上状态'], '批次实体E-R图')
    er_entity('4.17', '业务事件', ['事件编号', '事件类型', '内容哈希', '前序哈希'], '事件实体E-R图')


def fig_global_er():
    W, H = 5.7, 3.45
    f = Fig(W, H, '图4.18')
    bw, bh = 0.92, 0.44
    pos = {
        '组织': (0.10, 0.30), '批次': (2.10, 0.30), '业务事件': (4.10, 0.30),
        '用户': (0.10, 2.20), '交接单': (2.10, 2.20), '检验记录': (4.10, 2.20),
    }
    for k, (x, y) in pos.items():
        f.rect(x, y, bw, bh, k, size=9)

    def c(k):
        x, y = pos[k]
        return (x + bw / 2.0, y + bh / 2.0)

    # 拥有 组织1-用户n
    f.path([(c('组织')[0], 0.74), (c('组织')[0], 2.20)], arrow=False)
    f.diamond(0.25, 1.30, 0.62, 0.36, '拥有', size=8)
    f.tag(0.58, 0.80, 0.26, 0.22, '1', size=8)
    f.tag(0.58, 1.78, 0.26, 0.22, 'n', size=8)
    # 创建 组织1-批次n
    f.path([(1.02, 0.52), (2.10, 0.52)], arrow=False)
    f.diamond(1.28, 0.34, 0.62, 0.36, '创建', size=8)
    f.tag(1.04, 0.62, 0.24, 0.22, '1', size=8)
    f.tag(1.84, 0.62, 0.24, 0.22, 'n', size=8)
    # 包含 批次1-事件n
    f.path([(3.02, 0.52), (4.10, 0.52)], arrow=False)
    f.diamond(3.28, 0.34, 0.62, 0.36, '包含', size=8)
    f.tag(3.04, 0.62, 0.24, 0.22, '1', size=8)
    f.tag(3.84, 0.62, 0.24, 0.22, 'n', size=8)
    # 流转 批次1-交接单n
    f.path([(c('批次')[0], 0.74), (c('批次')[0], 2.20)], arrow=False)
    f.diamond(2.25, 1.30, 0.62, 0.36, '流转', size=8)
    f.tag(2.58, 0.80, 0.24, 0.22, '1', size=8)
    f.tag(2.58, 1.78, 0.24, 0.22, 'n', size=8)
    # 抽检 事件1-检验n
    f.path([(c('业务事件')[0], 0.74), (c('业务事件')[0], 2.20)], arrow=False)
    f.diamond(4.25, 1.30, 0.62, 0.36, '抽检', size=8)
    f.tag(4.58, 0.80, 0.24, 0.22, '1', size=8)
    f.tag(4.58, 1.78, 0.24, 0.22, 'n', size=8)
    # 提交 用户n-事件n（沿底部绕行）
    f.path([(c('用户')[0], 2.64), (c('用户')[0], 3.10), (5.30, 3.10),
            (5.30, 0.52), (5.02, 0.52)], arrow=False)
    f.diamond(2.60, 2.92, 0.62, 0.36, '提交', size=8)
    f.tag(1.30, 3.14, 0.26, 0.22, 'n', size=8)
    f.tag(5.34, 2.20, 0.24, 0.22, 'n', size=8)
    save(f, '4.18', '系统全局E-R图')



if __name__ == '__main__':
    which = sys.argv[1] if len(sys.argv) > 1 else 'all'
    steps = [
        ('arch', fig_arch), ('tree', fig_func_tree),
        ('login', fig_flow_login), ('batch', fig_flow_batch2), ('fill', fig_flow_fill),
        ('review', fig_flow_review), ('transfer', fig_flow_transfer),
        ('reg', fig_flow_reg), ('query', fig_flow_query), ('admin', fig_flow_admin),
        ('consumer', fig_flow_consumer),
        ('net', fig_network), ('block', fig_block), ('er', fig_er), ('ger', fig_global_er),
    ]
    for key, fn in steps:
        if which != 'all' and which != key:
            continue
        fn()
    quit_app()
