# -*- coding: utf-8 -*-
"""4.5 前端界面设计：界面原型图（线框图）"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from figlib import Fig, quit_app

BASE = r'E:\毕业项目实训\pakeage\docs'
VSDX = os.path.join(BASE, '图源文件')
PNG = os.path.join(BASE, '图片')

W, H = 5.2, 2.95


def save(f, no, name):
    stem = f'图{no}-{name}'
    f.save(os.path.join(VSDX, stem + '.vsdx'), os.path.join(PNG, f'fig{no.replace(".", "_")}.png'))
    print('ok', stem)


def frame(f, title, user='平台管理员'):
    f.rect(0.08, 0.08, W - 0.16, H - 0.16, '', fill=False)
    f.path([(0.08, 0.42), (W - 0.08, 0.42)], arrow=False)
    f.label(0.16, 0.11, 2.6, 0.28, '常德酱板鸭全链条可信溯源平台', size=8.5, align=0)
    f.label(W - 1.5, 0.11, 1.1, 0.28, user, size=8, align=2)


def sidebar(f, items, active=0):
    f.rect(0.08, 0.42, 0.94, H - 0.50, '', fill=False)
    for i, t in enumerate(items):
        y = 0.55 + i * 0.34
        f.rect(0.14, y, 0.82, 0.26, t, size=8, fill=(i == active))
        if i == active:
            f.label(0.14, y, 0.82, 0.26, t, size=8)


def table(f, x, y, w, cols, rows, rh=0.28, hh=0.26):
    n = len(cols)
    cw = w / n
    f.rect(x, y, w, hh, '', fill=False)
    for i, c in enumerate(cols):
        f.label(x + i * cw, y, cw, hh, c, size=7.5)
    for r in range(rows):
        ry = y + hh + r * rh
        f.rect(x, ry, w, rh, '', fill=False)
        if r == 0:
            for i in range(n):
                f.label(x + i * cw + 0.04, ry, cw - 0.08, rh, '——', size=7.5)
    return y + hh + rows * rh


def fig_login():
    f = Fig(W, H, 'p1')
    f.rect(0.08, 0.08, W - 0.16, H - 0.16, '', fill=False)
    f.label(0.08, 0.16, W - 0.16, 0.26, '常德酱板鸭全链条可信溯源平台', size=11, bold=True)
    f.rect(1.55, 0.62, 2.10, 1.95, '', fill=False)
    f.label(1.55, 0.68, 2.10, 0.24, '企业用户登录', size=9)
    f.rrect(1.72, 1.02, 1.76, 0.28, '请输入11位手机号', size=7.5)
    f.rrect(1.72, 1.40, 1.76, 0.28, '请输入登录密码', size=7.5)
    f.rrect(1.72, 1.80, 1.76, 0.30, '登 录', size=8.5)
    f.label(1.72, 2.16, 0.86, 0.22, '企业注册', size=7.5, align=0)
    f.label(2.62, 2.16, 0.86, 0.22, '消费者入口', size=7.5, align=2)
    f.label(0.12, 2.62, 4.9, 0.22, '提示：登录后按组织类型与岗位角色开放对应业务菜单', size=7.5, align=0)
    save(f, '4.19', '登录与注册界面原型')


def fig_dashboard():
    f = Fig(W, H, 'p2')
    frame(f, '')
    sidebar(f, ['工作台', '溯源批次', '创建批次', '我的提交', '审核待办'], 0)
    x0 = 1.12
    f.label(x0, 0.50, 2.2, 0.24, '工作台', size=9.5, align=0)
    cards = ['可见批次', '待审核', '待上链', '风险批次']
    cw = (5.00 - x0 - 0.03 * 3) / 4.0
    for i, c in enumerate(cards):
        cx = x0 + i * (cw + 0.03)
        f.rect(cx, 0.80, cw, 0.56, '', fill=False)
        f.label(cx, 0.84, cw, 0.2, c, size=7.5)
        f.label(cx, 1.02, cw, 0.26, '0', size=11)
    f.rect(x0, 1.48, 3.10, 1.30, '', fill=False)
    f.label(x0 + 0.04, 1.50, 2.0, 0.22, '最近批次', size=8, align=0)
    table(f, x0 + 0.04, 1.74, 3.02, ['批次号', '产品', '阶段', '状态'], 3, rh=0.26, hh=0.24)
    f.rect(4.32, 1.48, 0.68 + 0.72, 1.30, '', fill=False)
    f.label(4.36, 1.50, 1.2, 0.22, '岗位操作指引', size=8, align=0)
    for i in range(4):
        f.label(4.36, 1.76 + i * 0.24, 1.30, 0.22, '· 步骤  %d' % (i + 1), size=7.5, align=0)
    save(f, '4.20', '企业工作台界面原型')


def fig_batch_list():
    f = Fig(W, H, 'p3')
    frame(f, '')
    sidebar(f, ['工作台', '溯源批次', '创建批次', '我的提交', '审核待办'], 1)
    x0 = 1.12
    f.label(x0, 0.50, 2.4, 0.24, '溯源批次', size=9.5, align=0)
    f.rrect(x0, 0.82, 1.95, 0.28, '批次号/追溯码/产品名称', size=7.5)
    f.rrect(x0 + 2.02, 0.82, 0.85, 0.28, '全部状态', size=7.5)
    f.rrect(x0 + 2.95, 0.82, 0.55, 0.28, '查询', size=8)
    f.rrect(4.30, 0.82, 0.75, 0.28, '创建批次', size=8)
    table(f, x0, 1.20, 4.00, ['批次号', '产品', '所属企业', '当前阶段', '链上状态'], 4)
    f.label(x0, 2.56, 2.0, 0.22, '共 1 条  上一页 1 下一页', size=7.5, align=0)
    save(f, '4.21', '批次列表界面原型')


def fig_batch_create():
    f = Fig(W, H, 'p4')
    frame(f, '')
    sidebar(f, ['工作台', '溯源批次', '创建批次', '我的提交', '审核待办'], 2)
    x0 = 1.12
    f.label(x0, 0.50, 2.4, 0.24, '创建批次', size=9.5, align=0)
    fields = [('批次类型', '原料批次 / 加工批次'), ('上游批次', '选择已登记批次'),
              ('产品名称', '常德酱板鸭'), ('规格', '净重 180g'),
              ('数量 / 单位', '200 kg'), ('生产日期', '2026-11-01')]
    for i, (lb, ph) in enumerate(fields):
        col = i % 2
        row = i // 2
        x = x0 + col * 1.98
        y = 0.82 + row * 0.50
        f.label(x, y, 0.78, 0.24, lb, size=7.5, align=0)
        f.rrect(x + 0.78, y, 1.12, 0.26, ph, size=7.5)
    f.label(x0, 2.34, 1.0, 0.24, '备注', size=7.5, align=0)
    f.rrect(x0 + 0.78, 2.34, 3.20, 0.26, '选填：批次说明与关联说明', size=7.5)
    f.rrect(x0 + 2.55, 2.62, 0.62, 0.26, '提交', size=8)
    f.rrect(x0 + 3.25, 2.62, 0.62, 0.26, '取消', size=8)
    save(f, '4.22', '批次创建界面原型')


def fig_batch_detail():
    f = Fig(W, H, 'p5')
    frame(f, '')
    sidebar(f, ['批次详情', '业务填报', '交接单', '存证核验'], 1)
    x0 = 1.12
    f.label(x0, 0.50, 3.0, 0.24, '批次详情与业务填报', size=9.5, align=0)
    f.rect(x0, 0.80, 1.95, 1.90, '', fill=False)
    f.label(x0 + 0.06, 0.82, 1.8, 0.22, '产品履历（时间线）', size=8, align=0)
    steps = ['养殖与检疫', '屠宰腌制', '酱卤烘烤', '真空包装', '仓储运输']
    for i, s in enumerate(steps):
        y = 1.10 + i * 0.30
        f.ellipse(x0 + 0.10, y + 0.03, 0.12, 0.12, '', size=7)
        f.label(x0 + 0.30, y, 1.55, 0.22, s, size=7.5, align=0)
    f.rect(3.16, 0.80, 1.90, 1.90, '', fill=False)
    f.label(3.20, 0.82, 1.8, 0.22, '业务填报', size=8, align=0)
    f.label(3.20, 1.08, 0.70, 0.22, '业务类型', size=7.5, align=0)
    f.rrect(3.92, 1.08, 1.02, 0.24, '酱卤过程登记', size=7.5)
    for i, lb in enumerate(['配方版本', '实际温度', 'CCP结果']):
        f.label(3.20, 1.42 + i * 0.32, 0.70, 0.22, lb, size=7.5, align=0)
        f.rrect(3.92, 1.42 + i * 0.32, 1.02, 0.24, '请输入', size=7.5)
    f.rrect(3.20, 2.36, 0.80, 0.24, '上传附件', size=7.5)
    f.rrect(4.26, 2.36, 0.68, 0.24, '提交', size=8)
    save(f, '4.23', '批次详情与业务填报界面原型')


def fig_review():
    f = Fig(W, H, 'p6')
    frame(f, '')
    sidebar(f, ['工作台', '溯源批次', '我的提交', '审核待办'], 3)
    x0 = 1.12
    f.label(x0, 0.50, 2.4, 0.24, '审核待办', size=9.5, align=0)
    table(f, x0, 0.80, 2.35, ['提交人', '业务类型', '状态'], 4)
    f.rect(3.62, 0.80, 1.44, 1.90, '', fill=False)
    f.label(3.66, 0.82, 1.3, 0.22, '审核处理', size=8, align=0)
    f.label(3.66, 1.08, 1.32, 0.44, '填报摘要与附件\n哈希指纹', size=7.5)
    f.rrect(3.66, 1.58, 1.32, 0.30, '审核意见（驳回必填）', size=7)
    f.rrect(3.66, 1.96, 0.62, 0.26, '通过', size=8)
    f.rrect(4.36, 1.96, 0.62, 0.26, '驳回', size=8)
    f.label(3.66, 2.32, 1.32, 0.32, '通过后调用AppendEvent上链', size=7)
    save(f, '4.24', '审核待办界面原型')


def fig_admin():
    f = Fig(W, H, 'p7')
    frame(f, '')
    sidebar(f, ['工作台', '平台管理', '审计日志'], 1)
    x0 = 1.12
    f.label(x0, 0.50, 2.4, 0.24, '平台管理', size=9.5, align=0)
    names = ['原料供应商', '加工企业', '物流企业', '销售商', '监管部门']
    cw = (4.00 - 0.04 * 4) / 5.0
    for i, n in enumerate(names):
        cx = x0 + i * (cw + 0.04)
        f.rect(cx, 0.80, cw, 0.52, '', fill=False)
        f.label(cx + 0.03, 0.82, cw - 0.06, 0.22, n, size=7.5, align=0)
        f.label(cx + 0.03, 1.04, cw - 0.06, 0.22, 'ACTIVE', size=7, align=0)
    f.rect(x0, 1.48, 4.00, 1.20, '', fill=False)
    f.label(x0 + 0.04, 1.50, 2.0, 0.22, '企业账号与岗位角色', size=8, align=0)
    table(f, x0 + 0.04, 1.74, 3.92, ['账号', '姓名', '所属企业', '岗位角色', '操作'], 2, rh=0.26, hh=0.24)
    save(f, '4.25', '平台管理界面原型')


def fig_public():
    f = Fig(W, H, 'p8')
    f.rect(0.08, 0.08, W - 0.16, H - 0.16, '', fill=False)
    f.label(0.16, 0.12, 2.6, 0.26, '常德酱板鸭 · 全链条可信溯源', size=9, align=0)
    f.label(W - 1.45, 0.12, 1.3, 0.26, '消费者登录 / 注册', size=7.5, align=2)
    f.path([(0.08, 0.46), (W - 0.08, 0.46)], arrow=False)
    f.label(0.08, 0.52, W - 0.16, 0.26, '输入包装上的追溯码，查看原料、加工、物流与检验信息', size=8)
    f.rrect(1.35, 0.82, 2.10, 0.30, 'CDJBY-20261101002044-E8FAC5', size=8)
    f.rrect(3.50, 0.82, 0.62, 0.30, '查询', size=8)
    f.rect(0.20, 1.24, 3.10, 1.52, '', fill=False)
    f.label(0.24, 1.26, 2.0, 0.22, '产品经历（12个可信节点）', size=8, align=0)
    for i, s in enumerate(['已屠宰腌制', '已腌制', '已酱卤', '已烘烤杀菌', '已真空包装']):
        y = 1.52 + i * 0.24
        f.label(0.28, y, 0.30, 0.20, '0%d' % (i + 1), size=7)
        f.label(0.60, y, 1.55, 0.20, s, size=7.5, align=0)
        f.rrect(2.70, y, 0.52, 0.19, '已上链', size=6.5)
    f.rect(3.42, 1.24, 1.62, 1.52, '', fill=False)
    f.label(3.46, 1.26, 1.5, 0.22, '链上凭证', size=8, align=0)
    f.label(3.46, 1.52, 1.5, 0.30, '交易编号：TxID', size=7, align=0)
    f.label(3.46, 1.84, 1.5, 0.30, '内容哈希：SHA-256', size=7, align=0)
    f.label(3.46, 2.18, 1.5, 0.22, '检验结论：合格', size=7.5, align=0)
    save(f, '4.26', '公众溯源查询界面原型')


if __name__ == '__main__':
    for fn in (fig_login, fig_dashboard, fig_batch_list, fig_batch_create,
               fig_batch_detail, fig_review, fig_admin, fig_public):
        fn()
    quit_app()
