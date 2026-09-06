#!/usr/bin/env python3
"""
Generates minimal valid EPUB 3 fixtures for:
- Arabic RTL (page-progression-direction="rtl", dir="rtl", xml:lang="ar")
- Hebrew RTL (page-progression-direction="rtl", dir="rtl", xml:lang="he")
- Japanese CJK horizontal (page-progression-direction="ltr", xml:lang="ja")
- Japanese vertical-writing (page-progression-direction="rtl", writing-mode: vertical-rl, xml:lang="ja")
"""

import os
import zipfile

CONTAINER_XML = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
"""

def make_epub(output_path, opf_content, nav_content, ch1_content, ch2_content, css_content=None):
    with zipfile.ZipFile(output_path, "w") as z:
        # 1. mimetype uncompressed, no extra field
        m_info = zipfile.ZipInfo("mimetype")
        m_info.compress_type = zipfile.ZIP_STORED
        m_info.extra = b""
        z.writestr(m_info, "application/epub+zip")

        # 2. container.xml
        c_info = zipfile.ZipInfo("META-INF/container.xml")
        c_info.compress_type = zipfile.ZIP_DEFLATED
        c_info.extra = b""
        z.writestr(c_info, CONTAINER_XML.encode("utf-8"))

        # 3. content.opf
        opf_info = zipfile.ZipInfo("OEBPS/content.opf")
        opf_info.compress_type = zipfile.ZIP_DEFLATED
        opf_info.extra = b""
        z.writestr(opf_info, opf_content.encode("utf-8"))

        # 4. nav.xhtml
        nav_info = zipfile.ZipInfo("OEBPS/nav.xhtml")
        nav_info.compress_type = zipfile.ZIP_DEFLATED
        nav_info.extra = b""
        z.writestr(nav_info, nav_content.encode("utf-8"))

        # 5. ch1.xhtml
        ch1_info = zipfile.ZipInfo("OEBPS/ch1.xhtml")
        ch1_info.compress_type = zipfile.ZIP_DEFLATED
        ch1_info.extra = b""
        z.writestr(ch1_info, ch1_content.encode("utf-8"))

        # 6. ch2.xhtml
        ch2_info = zipfile.ZipInfo("OEBPS/ch2.xhtml")
        ch2_info.compress_type = zipfile.ZIP_DEFLATED
        ch2_info.extra = b""
        z.writestr(ch2_info, ch2_content.encode("utf-8"))

        # 7. style.css (optional)
        if css_content is not None:
            css_info = zipfile.ZipInfo("OEBPS/style.css")
            css_info.compress_type = zipfile.ZIP_DEFLATED
            css_info.extra = b""
            z.writestr(css_info, css_content.encode("utf-8"))


def generate_arabic_rtl(output_dir):
    opf = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="uid">urn:uuid:leafline-fixture-rtl-arabic</dc:identifier>
    <dc:title>Arabic RTL Test Book</dc:title>
    <dc:creator>Leafline Tests</dc:creator>
    <dc:language>ar</dc:language>
    <meta property="dcterms:modified">2026-09-06T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine page-progression-direction="rtl">
    <itemref idref="ch1"/>
    <itemref idref="ch2"/>
  </spine>
</package>
"""
    nav = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" dir="rtl" xml:lang="ar" lang="ar">
<head><title>المحتويات</title></head>
<body><nav epub:type="toc"><ol>
<li><a href="ch1.xhtml">الفصل الأول</a></li>
<li><a href="ch2.xhtml">الفصل الثاني</a></li>
</ol></nav></body></html>
"""
    ch1_paras = "".join(f"<p>الفقرة {i}. نص عربي لاختبار القراءة والملاحة من اليمين إلى اليسار في الفصل الأول.</p>" for i in range(1, 26))
    ch1 = f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" xml:lang="ar" lang="ar"><head><title>الفصل الأول</title></head>
<body><h1>الفصل الأول</h1>
<p>مرحبا بالعالم في اختبار القراءة باللغة العربية.</p>
{ch1_paras}
</body></html>
"""
    ch2_paras = "".join(f"<p>الفقرة {i}. يستمر النص العربي في واحة النخيل الجميلة خلال الفصل الثاني.</p>" for i in range(1, 26))
    ch2 = f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" xml:lang="ar" lang="ar"><head><title>الفصل الثاني</title></head>
<body><h1>الفصل الثاني</h1>
<p>مرحبا بكم في الفصل الثاني من هذا الكتاب.</p>
{ch2_paras}
</body></html>
"""
    out_file = os.path.join(output_dir, "rtl-arabic.epub")
    make_epub(out_file, opf, nav, ch1, ch2)
    print(f"Created {out_file}")


def generate_hebrew_rtl(output_dir):
    opf = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="uid">urn:uuid:leafline-fixture-rtl-hebrew</dc:identifier>
    <dc:title>Hebrew RTL Test Book</dc:title>
    <dc:creator>Leafline Tests</dc:creator>
    <dc:language>he</dc:language>
    <meta property="dcterms:modified">2026-09-06T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine page-progression-direction="rtl">
    <itemref idref="ch1"/>
    <itemref idref="ch2"/>
  </spine>
</package>
"""
    nav = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" dir="rtl" xml:lang="he" lang="he">
<head><title>תוכן עניינים</title></head>
<body><nav epub:type="toc"><ol>
<li><a href="ch1.xhtml">פרק ראשון</a></li>
<li><a href="ch2.xhtml">פרק שני</a></li>
</ol></nav></body></html>
"""
    ch1_paras = "".join(f"<p>פסקה {i}. טקסט עברי לבדיקת כיוון קריאה מימין לשמאל בפרק הראשון.</p>" for i in range(1, 26))
    ch1 = f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" xml:lang="he" lang="he"><head><title>פרק ראשון</title></head>
<body><h1>פרק ראשון</h1>
<p>שלום עולם בבדיקת קריאה בעברית.</p>
{ch1_paras}
</body></html>
"""
    ch2_paras = "".join(f"<p>פסקה {i}. המשך הטקסט העברי בפרק השני במסע הקריאה הדיגיטלי.</p>" for i in range(1, 26))
    ch2 = f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" dir="rtl" xml:lang="he" lang="he"><head><title>פרק שני</title></head>
<body><h1>פרק שני</h1>
<p>ברוכים הבאים לפרק השני של הספר.</p>
{ch2_paras}
</body></html>
"""
    out_file = os.path.join(output_dir, "rtl-hebrew.epub")
    make_epub(out_file, opf, nav, ch1, ch2)
    print(f"Created {out_file}")


def generate_cjk_japanese(output_dir):
    opf = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="uid">urn:uuid:leafline-fixture-cjk-japanese</dc:identifier>
    <dc:title>Japanese CJK Test Book</dc:title>
    <dc:creator>Leafline Tests</dc:creator>
    <dc:language>ja</dc:language>
    <meta property="dcterms:modified">2026-09-06T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine page-progression-direction="ltr">
    <itemref idref="ch1"/>
    <itemref idref="ch2"/>
  </spine>
</package>
"""
    nav = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="ja" lang="ja">
<head><title>目次</title></head>
<body><nav epub:type="toc"><ol>
<li><a href="ch1.xhtml">第一章</a></li>
<li><a href="ch2.xhtml">第二章</a></li>
</ol></nav></body></html>
"""
    ch1_paras = "".join(f"<p>段落 {i}。吾輩は猫である。名前はまだ無い。どこで生れたかとんと見当がつかぬ。</p>" for i in range(1, 26))
    ch1 = f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ja" lang="ja"><head><title>第一章</title></head>
<body><h1>第一章</h1>
<p>吾輩は猫である。名前はまだ無い。日本語の読書テストを開始します。</p>
{ch1_paras}
</body></html>
"""
    ch2_paras = "".join(f"<p>段落 {i}。山路を登りながら、こう考えた。智に働けば角が立つ。情に棹させば流される。</p>" for i in range(1, 26))
    ch2 = f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ja" lang="ja"><head><title>第二章</title></head>
<body><h1>第二章</h1>
<p>第二章の草枕の旅へようこそ。</p>
{ch2_paras}
</body></html>
"""
    out_file = os.path.join(output_dir, "cjk-japanese.epub")
    make_epub(out_file, opf, nav, ch1, ch2)
    print(f"Created {out_file}")


def generate_vertical_japanese(output_dir):
    opf = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="uid">urn:uuid:leafline-fixture-vertical-japanese</dc:identifier>
    <dc:title>Vertical Japanese Test Book</dc:title>
    <dc:creator>Leafline Tests</dc:creator>
    <dc:language>ja</dc:language>
    <meta property="dcterms:modified">2026-09-06T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="style" href="style.css" media-type="text/css"/>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine page-progression-direction="rtl">
    <itemref idref="ch1"/>
    <itemref idref="ch2"/>
  </spine>
</package>
"""
    css = """@charset "utf-8";
html {
  writing-mode: vertical-rl;
  -webkit-writing-mode: vertical-rl;
  -epub-writing-mode: vertical-rl;
}
"""
    nav = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="ja" lang="ja">
<head><title>目次</title></head>
<body><nav epub:type="toc"><ol>
<li><a href="ch1.xhtml">縦書き第一章</a></li>
<li><a href="ch2.xhtml">縦書き第二章</a></li>
</ol></nav></body></html>
"""
    ch1_paras = "".join(f"<p>段落 {i}。親譲りの無鉄砲で小供の時から損ばかりしている。縦書きの文章が右から左へ流れます。</p>" for i in range(1, 26))
    ch1 = f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ja" lang="ja">
<head>
  <title>縦書き第一章</title>
  <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body><h1>縦書き第一章</h1>
<p>親譲りの無鉄砲で小供の時から損ばかりしている。</p>
{ch1_paras}
</body></html>
"""
    ch2_paras = "".join(f"<p>段落 {i}。新築の二階から首を出していたら、同級生が冗談を言った。</p>" for i in range(1, 26))
    ch2 = f"""<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ja" lang="ja">
<head>
  <title>縦書き第二章</title>
  <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body><h1>縦書き第二章</h1>
<p>縦書き第二章のテスト本文です。</p>
{ch2_paras}
</body></html>
"""
    out_file = os.path.join(output_dir, "vertical-japanese.epub")
    make_epub(out_file, opf, nav, ch1, ch2, css_content=css)
    print(f"Created {out_file}")


if __name__ == "__main__":
    assets_dir = os.path.join(os.path.dirname(__file__), "..", "app", "src", "androidTest", "assets")
    os.makedirs(assets_dir, exist_ok=True)
    generate_arabic_rtl(assets_dir)
    generate_hebrew_rtl(assets_dir)
    generate_cjk_japanese(assets_dir)
    generate_vertical_japanese(assets_dir)
