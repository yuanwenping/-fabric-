import sys, docx
from docx.table import Table
from docx.text.paragraph import Paragraph
from docx.oxml.ns import qn

def iter_block_items(parent):
    body = parent.element.body
    for child in body.iterchildren():
        if child.tag == qn('w:p'):
            yield Paragraph(child, parent)
        elif child.tag == qn('w:tbl'):
            yield Table(child, parent)
        elif child.tag == qn('w:sectPr'):
            yield 'SECTPR'

path = sys.argv[1]
d = docx.Document(path)
print("=== SECTIONS ===")
for s in d.sections:
    print("page", s.page_width, s.page_height, "margins", s.left_margin, s.right_margin, s.top_margin, s.bottom_margin)

i = 0
for b in iter_block_items(d):
    if b == 'SECTPR':
        print(f"[{i}] <<SECTPR>>")
        i += 1
        continue
    if isinstance(b, Paragraph):
        txt = b.text.replace('\n', ' ')
        has_img = 'graphic' in b._p.xml
        # detect drawing count
        drawings = b._p.xml.count('<w:drawing')
        pf = b.paragraph_format
        info = f"[{i}] P style={b.style.name!r}"
        if drawings:
            info += f" IMG x{drawings}"
        if txt.strip():
            info += f" | {txt[:160]}"
        print(info)
    else:
        rows = len(b.rows); cols = len(b.columns)
        print(f"[{i}] TABLE style={b.style.name if b.style else None!r} rows={rows} cols={cols}")
        for r in b.rows[:3]:
            cells = [c.text.replace('\n','/')[:22] for c in r.cells]
            print("      | " + " || ".join(cells))
        sf = b._tbl.xml[:0]
    i += 1
print("total blocks", i)
