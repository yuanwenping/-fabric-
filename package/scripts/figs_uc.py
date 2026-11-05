# -*- coding: utf-8 -*-
"""Ch3 用例图：每个角色单独一张"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from figlib import Fig, quit_app

BASE = r'E:\毕业项目实训\pakeage\docs'
VSDX = os.path.join(BASE, '图源文件')
PNG = os.path.join(BASE, '图片')
os.makedirs(VSDX, exist_ok=True)
os.makedirs(PNG, exist_ok=True)


def usecase(no, boundary_title, actor_text, cases, actor_label_w=1.0):
    n = len(cases)
    step = 0.40 if n > 7 else 0.44
    eh = 0.38
    w = 4.35
    h = 0.62 + n * step + 0.14
    f = Fig(w, h, f'图3.{no}')
    f.boundary(1.32, 0.12, w - 1.40, h - 0.22, boundary_title, size=10.5)
    ay = 0.55 + (n * step) / 2.0 - 0.62
    ax = 0.16
    anchor = f.actor(ax, ay, actor_text, size=10.5, label_w=actor_label_w)
    for i, c in enumerate(cases):
        y = 0.55 + i * step
        f.ellipse(1.50, y, 2.62, eh, c, size=9.5)
        f.arrow(anchor[0] + 0.16, anchor[1], 1.50, y + eh / 2.0)
    stem = f'图3.{no}-{actor_text}用例图'
    f.save(os.path.join(VSDX, stem + '.vsdx'), os.path.join(PNG, f'fig3_{no}.png'))
    print('ok', stem)


USECASES = [
    ('原料供应商', '原料供应商', [
        '养殖批次登记', '养殖过程记录', '检疫申报登记', '原料批次创建',
        '内部质量自检', '发货交接登记', '批次进度查询', '员工账号管理']),
    ('加工企业', '加工企业', [
        '上游批次收货', '屠宰环节登记', '腌制过程登记', '酱卤过程登记',
        '烘烤杀菌登记', '真空包装登记', '成品出入库登记', '加工批次创建',
        '生产质量自检']),
    ('物流企业', '物流企业', [
        '运输任务调度', '在途运输登记', '温控异常上报', '到货交付签收', '批次状态查询']),
    ('销售商', '销售商', [
        '门店收货确认', '入库上架登记', '出库销售登记', '追溯码核验',
        '销售数据上报', '批次信息查询']),
    ('监管部门', '监管部门', [
        '监督抽检登记', '检验结论录入', '批次风险冻结', '解除冻结',
        '产品召回处置', '全链条追溯核查', '存证一致性核验']),
    ('平台管理员', '平台管理员', [
        '企业注册审核', '组织信息维护', '账号创建启停', '密码重置',
        '岗位角色分配', '审计日志查看', '链码连通检查']),
    ('消费者', '消费者', [
        '追溯码查询', '产品履历查看', '检验结论查看', '收藏产品',
        '查询历史记录', '个人资料维护']),
]

for i, (bt, at, cs) in enumerate(USECASES, start=1):
    usecase(i, bt + '业务子系统', at, cs, actor_label_w=1.1)

quit_app()
