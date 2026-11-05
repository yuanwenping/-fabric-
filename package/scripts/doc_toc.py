# -*- coding: utf-8 -*-
"""用 Word 更新目录域并导出 PDF 以便校验"""
import os
import sys
import time

try:
    import pythoncom
    import win32com.client as win32
except ImportError:
    sys.exit('need pywin32')

DOC = r'E:/没用的的东西/2026下学期实训/专业综合实践-课程设计作品-202301190144-袁文平-实训作品.docx'
PDF = r'E:/毕业项目实训/pakeage/docs/预览/实训作品预览.pdf'


def main():
    os.makedirs(os.path.dirname(PDF), exist_ok=True)
    pythoncom.CoInitialize()
    app = win32.gencache.EnsureDispatch('Word.Application')
    app.Visible = False
    app.DisplayAlerts = 0
    app.ScreenUpdating = False
    doc = app.Documents.Open(DOC.replace('/', '\\'))
    try:
        doc.Fields.Update()
        for i in range(1, doc.TablesOfContents.Count + 1):
            doc.TablesOfContents(i).Update()
        doc.Repaginate()
        time.sleep(1)
        pages = doc.ComputeStatistics(2)   # wdStatisticPages
        words = doc.ComputeStatistics(0)
        print('pages', pages, 'words', words)
        doc.Save()
        doc.ExportAsFixedFormat(PDF.replace('/', '\\'), 17)
        print('pdf ok')
    finally:
        doc.Close(0)
        app.Quit()


if __name__ == '__main__':
    main()
