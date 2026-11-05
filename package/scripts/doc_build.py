# -*- coding: utf-8 -*-
"""按模板组装最终文档：专业综合实践-课程设计作品-202301190144-袁文平-实训作品.docx"""
import copy
import os
import sys

import docx
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.shared import Cm
from docx.text.paragraph import Paragraph

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from doc_content1 import CH12
from doc_content2 import CH3
from doc_content3 import CH4
from doc_content4 import CH5, SHOT_SECTION, CH6, CH7, REFS

_split = next(i for i, it in enumerate(CH12) if it[0] == 'h1' and it[1].startswith('2'))
CH1, CH2 = CH12[:_split], CH12[_split:]

TPL = r'E:/没用的的东西/2026下学期实训/专业综合实践-课程设计作品-202201190XXX-姓名.docx'
IMGDIR = r'E:/毕业项目实训/pakeage/docs/图片'
SHOTDIR = r'E:/毕业项目实训/pakeage/docs/截图'
OUT = r'E:/没用的的东西/2026下学期实训/专业综合实践-课程设计作品-202301190144-袁文平-实训作品.docx'

SHOT_MAP = {
    '@shot_dashboard': ('shot_dashboard.png', 'fig4_20.png'),
    '@shot_detail': ('shot_detail.png', 'fig4_23.png'),
    '@shot_review': ('shot_review.png', 'fig4_24.png'),
    '@shot_public': ('shot_public.png', 'fig4_26.png'),
}


def strip_sectpr(p_el):
    pPr = p_el.find(qn('w:pPr'))
    if pPr is None:
        return
    for s in pPr.findall(qn('w:sectPr')):
        pPr.remove(s)


def set_para_text(p_el, text):
    """保留第一个 run 的格式，写入新文本"""
    runs = p_el.findall(qn('w:r'))
    if not runs:
        r = OxmlElement('w:r')
        p_el.append(r)
        runs = [r]
    first = runs[0]
    for r in runs[1:]:
        p_el.remove(r)
    for t in first.findall(qn('w:t')):
        first.remove(t)
    for br in first.findall(qn('w:br')):
        first.remove(br)
    t = OxmlElement('w:t')
    t.set(qn('xml:space'), 'preserve')
    t.text = text
    first.append(t)
    return p_el


def clone_para(tpl_el, text=''):
    el = copy.deepcopy(tpl_el)
    strip_sectpr(el)
    # 去掉可能存在的域与书签
    for tag in ('w:bookmarkStart', 'w:bookmarkEnd'):
        for b in el.findall(qn(tag)):
            el.remove(b)
    set_para_text(el, text)
    return el


def build_table(tpl_tbl, rows, widths):
    tbl = OxmlElement('w:tbl')
    tblPr = tpl_tbl.find(qn('w:tblPr'))
    tbl.append(copy.deepcopy(tblPr))
    grid = OxmlElement('w:tblGrid')
    for w in widths:
        gc = OxmlElement('w:gridCol')
        gc.set(qn('w:w'), str(int(w * 9000)))
        grid.append(gc)
    tbl.append(grid)

    trs = tpl_tbl.findall(qn('w:tr'))
    head_tpl = trs[0]
    body_tpl = trs[1] if len(trs) > 1 else trs[0]
    head_cell_tpl = head_tpl.findall(qn('w:tc'))[0]
    body_cell_tpl = body_tpl.findall(qn('w:tc'))[0]

    for ri, row in enumerate(rows):
        base_tr = head_tpl if ri == 0 else body_tpl
        cell_tpl = head_cell_tpl if ri == 0 else body_cell_tpl
        tr = OxmlElement('w:tr')
        trPr = base_tr.find(qn('w:trPr'))
        if trPr is not None:
            tr.append(copy.deepcopy(trPr))
        for ci, val in enumerate(row):
            tc = copy.deepcopy(cell_tpl)
            tcPr = tc.find(qn('w:tcPr'))
            if tcPr is not None:
                for old in tcPr.findall(qn('w:tcW')):
                    tcPr.remove(old)
                tcW = OxmlElement('w:tcW')
                tcW.set(qn('w:w'), str(int(widths[ci] * 5000)))
                tcW.set(qn('w:type'), 'pct')
                tcPr.insert(0, tcW)
            ps = tc.findall(qn('w:p'))
            for extra in ps[1:]:
                tc.remove(extra)
            set_para_text(ps[0], val)
            tr.append(tc)
        tbl.append(tr)
    return tbl


def main():
    doc = docx.Document(TPL)
    body = doc.element.body
    children = list(body)

    # ---- 取格式模板 ----
    tpl_p = children[97]          # 正文段落
    tpl_img = children[188]       # 图片段落
    tpl_figcap = children[189]    # 图题
    tpl_tblcap = children[237]    # 表题
    tpl_ref = children[427]       # 参考文献条目
    tpl_h1 = children[95]
    tpl_h2 = children[96] if children[96].find(qn('w:pPr')) is not None else children[95]
    tpl_h2 = children[121]        # '2 相关技术介绍' 实为 Heading1；改用 122
    tpl_h2 = children[122]        # '2.1 区块链技术' Heading 2
    tpl_h3 = children[125]        # '2.1.1 区块链的定义' Heading 3
    tpl_tbl3 = children[238]
    tpl_tbl4 = children[377]
    tpl_tbl6 = children[266]

    # ---- 保存分节锚点 ----
    anchor_ch1 = children[120]
    anchor_ch2 = children[146]
    anchor_ch34 = children[308]
    anchor_ch5 = children[367]
    anchor_ch6 = children[418]
    anchor_ch7 = children[444]
    final_sectpr = children[457]
    sectpr_body = copy.deepcopy(anchor_ch1.find(qn('w:pPr')).find(qn('w:sectPr')))
    for pn in sectpr_body.findall(qn('w:pgNumType')):
        sectpr_body.remove(pn)

    # ---- 删除模板示例与说明（正文部分）----
    keep = {120, 146, 308, 367, 418, 444}
    for i in range(95, 457):
        if i in keep:
            continue
        children[i].getparent().remove(children[i])
    # 末节 sectPr 改为正文样式
    body.remove(final_sectpr)
    body.append(copy.deepcopy(sectpr_body))
    # 合并最后一节：去掉 444 段落内的 sectPr
    strip_sectpr(anchor_ch7)
    # 清空各锚点段落文字
    for a in (anchor_ch1, anchor_ch2, anchor_ch34, anchor_ch5, anchor_ch6, anchor_ch7):
        set_para_text(a, '')

    # ---- 目录域：删除静态目录条目，插入 TOC 域 ----
    toc_par = None
    for el in list(body):
        if el.tag != qn('w:p'):
            continue
        txt = ''.join(t.text or '' for t in el.iter(qn('w:t')))
        if txt.replace(' ', '').startswith('目录'):
            toc_par = el
            break
    assert toc_par is not None, '未找到目录标题'
    stop_el = children[92]              # '红色字符请最后删除'
    sib = toc_par.getnext()
    while sib is not None and sib is not stop_el:
        nxt = sib.getnext()
        body.remove(sib)
        sib = nxt
    for i in range(89, 94):             # 红色说明文字
        el = children[i]
        if el.getparent() is not None:
            el.getparent().remove(el)

    field_p = OxmlElement('w:p')
    r1 = OxmlElement('w:r')
    fc = OxmlElement('w:fldChar')
    fc.set(qn('w:fldCharType'), 'begin')
    fc.set(qn('w:dirty'), 'true')
    r1.append(fc)
    r2 = OxmlElement('w:r')
    it = OxmlElement('w:instrText')
    it.set(qn('xml:space'), 'preserve')
    it.text = ' TOC \\o "1-3" \\h \\z \\u '
    r2.append(it)
    r3 = OxmlElement('w:r')
    sep = OxmlElement('w:fldChar')
    sep.set(qn('w:fldCharType'), 'separate')
    r3.append(sep)
    r4 = OxmlElement('w:r')
    t4 = OxmlElement('w:t')
    t4.text = '（请在 Word 中更新目录）'
    r4.append(t4)
    r5 = OxmlElement('w:r')
    end = OxmlElement('w:fldChar')
    end.set(qn('w:fldCharType'), 'end')
    r5.append(end)
    for r in (r1, r2, r3, r4, r5):
        field_p.append(r)
    if toc_par is not None:
        toc_par.addnext(field_p)

    # ---- 内容写入 ----
    counters = {'img': 0, 'tbl': 0}

    def insert_block(anchor, item):
        kind = item[0]
        if kind in ('h1', 'h2', 'h3'):
            tpl = {'h1': tpl_h1, 'h2': tpl_h2, 'h3': tpl_h3}[kind]
            el = clone_para(tpl, item[1])
            anchor.addprevious(el)
        elif kind == 'p':
            el = clone_para(tpl_p, item[1])
            anchor.addprevious(el)
        elif kind == 'ref':
            el = clone_para(tpl_ref, item[1])
            anchor.addprevious(el)
        elif kind == 'cap':
            el = clone_para(tpl_figcap, item[1])
            anchor.addprevious(el)
        elif kind == 'tcap':
            el = clone_para(tpl_tblcap, item[1])
            anchor.addprevious(el)
        elif kind == 'img':
            path = item[1]
            if path.startswith('@shot'):
                real, fallback = SHOT_MAP[path]
                p1 = os.path.join(SHOTDIR, real)
                path = p1 if os.path.exists(p1) else fallback
            if not os.path.isabs(path):
                path = os.path.join(IMGDIR, path)
            el = copy.deepcopy(tpl_img)
            strip_sectpr(el)
            for r in el.findall(qn('w:r')):
                el.remove(r)
            for b in el.findall(qn('w:bookmarkStart')):
                el.remove(b)
            anchor.addprevious(el)
            para = Paragraph(el, doc)
            para.add_run().add_picture(path, width=Cm(item[2]))
        elif kind == 'tbl':
            rows, widths = item[1], item[2]
            tpl = tpl_tbl6 if len(widths) >= 6 else (tpl_tbl4 if len(widths) == 4 else tpl_tbl3)
            tbl = build_table(tpl, rows, widths)
            anchor.addprevious(tbl)
        else:
            raise ValueError(kind)

    for item in CH1:
        insert_block(anchor_ch1, item)
    for item in CH2:
        insert_block(anchor_ch2, item)
    for item in CH3 + CH4:
        insert_block(anchor_ch34, item)
    for item in CH5 + SHOT_SECTION:
        insert_block(anchor_ch5, item)
    for item in CH6:
        insert_block(anchor_ch6, item)
    for item in CH7 + REFS:
        insert_block(anchor_ch7, item)

    doc.save(OUT)
    print('saved', OUT)
    print('paragraphs', len(doc.paragraphs), 'tables', len(doc.tables))


if __name__ == '__main__':
    main()
