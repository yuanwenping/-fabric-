import sys, docx
from docx.oxml.ns import qn
from lxml import etree

path = sys.argv[1]
idxs = [int(x) for x in sys.argv[2].split(',')]
d = docx.Document(path)
body = d.element.body
children = [c for c in body.iterchildren()]
for i in idxs:
    el = children[i]
    xml = etree.tostring(el, pretty_print=True).decode()
    print(f"===== BLOCK {i} ({el.tag.split('}')[1]}) len={len(xml)} =====")
    print(xml[:3000])
