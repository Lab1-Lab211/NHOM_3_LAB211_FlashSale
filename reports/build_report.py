from pathlib import Path
import csv
import json

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.style import WD_STYLE_TYPE
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parent.parent
REPORT_DIR = ROOT / "reports"
EXP_DIR = REPORT_DIR / "experiment"
FIG_DIR = EXP_DIR / "figures"
OUT = REPORT_DIR / "FlashSale_Concurrency_Experiment_Report.docx"

analysis = json.loads((EXP_DIR / "analysis_summary.json").read_text(encoding="utf-8"))
summary = {row["mechanism"]: row for row in analysis["summary"]}
with (EXP_DIR / "raw_results_1000_threads_4_mechanisms_3_repeats.csv").open(
        encoding="utf-8-sig", newline="") as stream:
    raw_rows = list(csv.DictReader(stream))

BLUE = "2563EB"
BLUE_DARK = "1D4ED8"
NAVY = "101828"
GRAY = "475467"
LIGHT = "F2F4F7"
LIGHT_BLUE = "EFF6FF"
GREEN = "027A48"
GREEN_LIGHT = "ECFDF3"
RED = "B42318"
RED_LIGHT = "FEF3F2"
TEAL = "0E9384"
PURPLE = "7A5AF8"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_border(cell, **edges):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_borders = tc_pr.first_child_found_in("w:tcBorders")
    if tc_borders is None:
        tc_borders = OxmlElement("w:tcBorders")
        tc_pr.append(tc_borders)
    for edge_name, edge_data in edges.items():
        edge = tc_borders.find(qn(f"w:{edge_name}"))
        if edge is None:
            edge = OxmlElement(f"w:{edge_name}")
            tc_borders.append(edge)
        for key, value in edge_data.items():
            edge.set(qn(f"w:{key}"), str(value))


def set_cell_margins(cell, top=90, start=100, bottom=90, end=100):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for margin, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{margin}"))
        if node is None:
            node = OxmlElement(f"w:{margin}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def add_field(paragraph, instruction):
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = instruction
    separate = OxmlElement("w:fldChar")
    separate.set(qn("w:fldCharType"), "separate")
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.extend([begin, instr, separate, end])


def add_hyperlink(paragraph, text_value, url, color=BLUE, underline=True):
    part = paragraph.part
    relation_id = part.relate_to(url,
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink",
            is_external=True)
    hyperlink = OxmlElement("w:hyperlink")
    hyperlink.set(qn("r:id"), relation_id)
    run = OxmlElement("w:r")
    run_props = OxmlElement("w:rPr")
    color_node = OxmlElement("w:color")
    color_node.set(qn("w:val"), color)
    run_props.append(color_node)
    if underline:
        underline_node = OxmlElement("w:u")
        underline_node.set(qn("w:val"), "single")
        run_props.append(underline_node)
    run.append(run_props)
    text_node = OxmlElement("w:t")
    text_node.text = text_value
    run.append(text_node)
    hyperlink.append(run)
    paragraph._p.append(hyperlink)


def set_run_font(run, name="Calibri", size=None, bold=None, color=None, italic=None):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic
    if color:
        run.font.color.rgb = RGBColor.from_string(color)


def paragraph(text_value="", style=None, bold_prefix=None):
    p = doc.add_paragraph(style=style)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.line_spacing = 1.08
    if bold_prefix and text_value.startswith(bold_prefix):
        first, rest = text_value.split(":", 1)
        r = p.add_run(first + ":")
        r.bold = True
        p.add_run(rest)
    else:
        p.add_run(text_value)
    return p


def bullet(text_value, level=0):
    p = doc.add_paragraph()
    indent = 0.18 + 0.18 * level
    p.paragraph_format.left_indent = Inches(indent)
    p.paragraph_format.first_line_indent = Inches(-0.18)
    p.paragraph_format.space_after = Pt(4)
    marker = p.add_run("•  ")
    set_run_font(marker, bold=True, color=BLUE)
    p.add_run(text_value)
    return p


def numbered(text_value):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Inches(0.18)
    p.paragraph_format.first_line_indent = Inches(-0.18)
    p.paragraph_format.space_after = Pt(4)
    marker = p.add_run("•  ")
    set_run_font(marker, bold=True, color=BLUE)
    p.add_run(text_value)
    return p


def callout(title, body, fill=LIGHT_BLUE, accent=BLUE):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.columns[0].width = Inches(6.65)
    cell = table.cell(0, 0)
    set_cell_shading(cell, fill)
    set_cell_margins(cell, top=150, start=220, bottom=150, end=220)
    set_cell_border(cell, left={"val": "single", "sz": "22", "color": accent})
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run(title)
    set_run_font(r, size=11, bold=True, color=accent)
    p2 = cell.add_paragraph(body)
    p2.paragraph_format.space_after = Pt(0)
    p2.paragraph_format.line_spacing = 1.08
    for r2 in p2.runs:
        set_run_font(r2, size=10.5, color=NAVY)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def code_block(lines):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.columns[0].width = Inches(6.65)
    cell = table.cell(0, 0)
    set_cell_shading(cell, "F8FAFC")
    set_cell_border(cell,
        top={"val": "single", "sz": "6", "color": "D0D5DD"},
        bottom={"val": "single", "sz": "6", "color": "D0D5DD"},
        left={"val": "single", "sz": "6", "color": "D0D5DD"},
        right={"val": "single", "sz": "6", "color": "D0D5DD"})
    set_cell_margins(cell, top=140, start=180, bottom=140, end=180)
    p = cell.paragraphs[0]
    p.paragraph_format.space_after = Pt(0)
    for index, line_value in enumerate(lines):
        if index:
            p.add_run().add_break()
        r = p.add_run(line_value)
        set_run_font(r, name="Consolas", size=8.5, color="344054")


def make_table(headers, rows, widths=None, font_size=8.5, header_fill=BLUE_DARK):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.style = "Table Grid"
    hdr = table.rows[0].cells
    for idx, value in enumerate(headers):
        hdr[idx].text = str(value)
        set_cell_shading(hdr[idx], header_fill)
        hdr[idx].vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        set_cell_margins(hdr[idx], top=80, start=70, bottom=80, end=70)
        p = hdr[idx].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        for run in p.runs:
            set_run_font(run, size=font_size, bold=True, color="FFFFFF")
    for row_index, row in enumerate(rows):
        cells = table.add_row().cells
        for idx, value in enumerate(row):
            cells[idx].text = str(value)
            cells[idx].vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_cell_margins(cells[idx], top=65, start=65, bottom=65, end=65)
            if row_index % 2:
                set_cell_shading(cells[idx], "F9FAFB")
            p = cells[idx].paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT if idx == 0 else WD_ALIGN_PARAGRAPH.CENTER
            for run in p.runs:
                set_run_font(run, size=font_size, color=NAVY)
        if widths:
            for idx, width in enumerate(widths):
                cells[idx].width = Inches(width)
    if widths:
        for idx, width in enumerate(widths):
            table.columns[idx].width = Inches(width)
    return table


def caption(text_value):
    p = doc.add_paragraph(style="Caption")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run(text_value)
    return p


def add_figure(path, caption_text, width=6.75):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(4)
    p.add_run().add_picture(str(path), width=Inches(width))
    caption(caption_text)


def new_page(title, subtitle=None, number=None):
    doc.add_page_break()
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(2)
    if number:
        r = p.add_run(number.upper())
        set_run_font(r, size=9, bold=True, color=BLUE)
    h = doc.add_paragraph(style="Title")
    h.paragraph_format.space_after = Pt(8)
    r = h.add_run(title)
    set_run_font(r, size=24, bold=True, color=NAVY)
    if subtitle:
        s = doc.add_paragraph()
        s.paragraph_format.space_after = Pt(12)
        r2 = s.add_run(subtitle)
        set_run_font(r2, size=11, color=GRAY)


doc = Document()
section = doc.sections[0]
section.page_width = Inches(8.5)
section.page_height = Inches(11)
section.top_margin = Inches(0.7)
section.bottom_margin = Inches(0.7)
section.left_margin = Inches(0.8)
section.right_margin = Inches(0.8)
section.header_distance = Inches(0.3)
section.footer_distance = Inches(0.35)
section.different_first_page_header_footer = True

styles = doc.styles
normal = styles["Normal"]
normal.font.name = "Calibri"
normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Calibri")
normal.font.size = Pt(10.5)
normal.font.color.rgb = RGBColor.from_string(NAVY)
normal.paragraph_format.space_after = Pt(6)
normal.paragraph_format.line_spacing = 1.08

for style_name, size, color in (("Title", 24, NAVY), ("Heading 1", 17, BLUE_DARK),
                                ("Heading 2", 13, NAVY), ("Heading 3", 11, NAVY)):
    style = styles[style_name]
    style.font.name = "Calibri"
    style._element.rPr.rFonts.set(qn("w:eastAsia"), "Calibri")
    style.font.size = Pt(size)
    style.font.bold = True
    style.font.color.rgb = RGBColor.from_string(color)
    style.paragraph_format.keep_with_next = True
    style.paragraph_format.space_before = Pt(8)
    style.paragraph_format.space_after = Pt(5)

styles["Caption"].font.name = "Calibri"
styles["Caption"]._element.rPr.rFonts.set(qn("w:eastAsia"), "Calibri")
styles["Caption"].font.size = Pt(8.5)
styles["Caption"].font.italic = True
styles["Caption"].font.color.rgb = RGBColor.from_string(GRAY)

for style_name in ("List Bullet", "List Bullet 2", "List Number"):
    styles[style_name].font.name = "Calibri"
    styles[style_name]._element.rPr.rFonts.set(qn("w:eastAsia"), "Calibri")
    styles[style_name].font.size = Pt(10.5)

# Header and footer.
header = section.header
p = header.paragraphs[0]
p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
r = p.add_run("FLASHSALE CSV CONCURRENCY EXPERIMENT")
set_run_font(r, size=8.5, bold=True, color=GRAY)
footer = section.footer
p = footer.paragraphs[0]
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("Nhóm 3 · LAB211 FlashSale  |  ")
set_run_font(r, size=8, color=GRAY)
r = p.add_run("Concurrency Experiment")
set_run_font(r, size=8, color=GRAY)

# Cover.
doc.core_properties.title = "FlashSale CSV Concurrency Experiment Report"
doc.core_properties.subject = "1000 threads × 4 mechanisms × 3 repeats"
doc.core_properties.author = "Nhóm 3 – LAB211 FlashSale"
doc.core_properties.keywords = "CSV, concurrency, race condition, synchronization, throughput, negative stock"

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(85)
r = p.add_run("LAB211 · NHÓM 3")
set_run_font(r, size=11, bold=True, color=BLUE)

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(18)
r = p.add_run("BÁO CÁO THÍ NGHIỆM\nĐỒNG THỜI TRÊN CSV")
set_run_font(r, size=29, bold=True, color=NAVY)

p = doc.add_paragraph()
p.paragraph_format.space_after = Pt(36)
r = p.add_run("1.000 threads × 4 cơ chế × 3 lần lặp")
set_run_font(r, size=16, bold=True, color=BLUE_DARK)

callout("BIG QUESTION",
        "Khi nhiều luồng xử lý đơn hàng đồng thời trên cùng một file CSV kho hàng, race condition nào xảy ra, và kỹ thuật đồng bộ hóa nào ngăn được tình trạng âm kho mà không làm throughput giảm quá 30%?",
        fill=LIGHT_BLUE, accent=BLUE)

p = doc.add_paragraph()
p.paragraph_format.space_before = Pt(42)
for label, value in (("Hệ thống", "NHOM_3_LAB211_FlashSale"),
                     ("Đối tượng", "FSI-00167 · limitedQty = 96"),
                     ("Phiên bản nguồn", "Git commit a5a83dc"),
                     ("Ngày thực nghiệm", "21/07/2026")):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(3)
    r1 = p.add_run(f"{label}: ")
    set_run_font(r1, size=10, bold=True, color=GRAY)
    r2 = p.add_run(value)
    set_run_font(r2, size=10, color=NAVY)

# Executive answer.
new_page("Tóm tắt điều hành", "Kết luận trước, bằng chứng theo sau", "01 · Executive summary")
callout("CÂU TRẢ LỜI NGẮN",
        "Ba cơ chế FILE_LOCK, SYNCHRONIZED và OPTIMISTIC đều đạt tiêu chí định lượng: 0% âm kho, 0% sai lệch nhất quán và throughput trung bình không thấp hơn 70% của baseline SYNCHRONIZED. NO_LOCK không đạt vì 90,4% giao dịch thành công vượt quá tồn kho logic và 99,9% cập nhật bị sai lệch.",
        fill=GREEN_LIGHT, accent=GREEN)
paragraph("Khuyến nghị triển khai: dùng SYNCHRONIZED cho ứng dụng CSV chạy trong một JVM. Cơ chế này bán đủ 96/96 đơn vị, không mất cập nhật, và là mốc hiệu năng an toàn 49,9 TPS. OPTIMISTIC cũng an toàn nhưng chậm hơn trung bình 20,3% trong kịch bản tất cả luồng tranh chấp cùng một item.")
paragraph("FILE_LOCK đạt bài kiểm tra hai trục một cách hình thức với 160,8 TPS và 0% âm kho, nhưng chỉ bán được trung bình 58,3/96 đơn vị. Oracle nêu rõ FileLock không phù hợp để điều phối nhiều thread trong cùng JVM; các lần khóa chồng lấn có thể ném OverlappingFileLockException [R2][R3]. Vì vậy TPS cao ở đây bị khuếch đại bởi fail-fast, không đồng nghĩa chất lượng xử lý tốt.")
make_table(
    ["Cơ chế", "TPS TB", "Âm kho", "Nhất quán", "Dùng kho", "vs SYNC", "RQ"],
    [[name,
      f"{float(summary[name]['meanThroughputTps']):.1f}",
      f"{float(summary[name]['meanNegativeStockRatePct']):.1f}%",
      f"{float(summary[name]['meanConsistencyViolationRatePct']):.1f}%",
      f"{float(summary[name]['inventoryUtilizationPct']):.1f}%",
      f"{float(summary[name]['vsSynchronizedMeanPct']):+.1f}%",
      "ĐẠT" if summary[name]["rqPass"] == "true" else "KHÔNG"]
     for name in ("NO_LOCK", "FILE_LOCK", "SYNCHRONIZED", "OPTIMISTIC")],
    widths=[1.25, .72, .72, .8, .72, .72, .62], font_size=7.8)

# TOC.
new_page("Mục lục", "Cấu trúc báo cáo và phụ lục tái lập", "02 · Contents")
contents = [
    ("1–7", "Bối cảnh, hệ thống, race condition, cơ chế, giả thuyết, thiết kế và metric"),
    ("8–12", "Kết quả thô, kết quả trung bình và ba biểu đồ phân tích"),
    ("13–16", "Phân tích riêng NO_LOCK, FILE_LOCK, SYNCHRONIZED, OPTIMISTIC"),
    ("17–20", "Trả lời RQ, baseline sensitivity, threats to validity, khuyến nghị"),
    ("21–22", "Tái lập thí nghiệm và kết luận"),
    ("Phụ lục", "Tài liệu tham khảo và đường dẫn dữ liệu"),
]
make_table(["Phần", "Nội dung"], contents, widths=[1.15, 5.3], font_size=9)
paragraph("Báo cáo được tổ chức theo mạch: câu hỏi → thiết kế → bằng chứng → phân tích → quyết định. Mỗi chương lớn bắt đầu ở trang mới để thuận tiện trình bày và phản biện.")

# Problem.
new_page("1. Bối cảnh và vấn đề nghiên cứu", "CSV là shared mutable state nhưng thao tác đọc–kiểm tra–ghi không nguyên tử", "03 · Problem")
doc.add_heading("1.1 Bối cảnh", level=1)
paragraph("Hệ thống FlashSale lưu tồn kho của từng sản phẩm trong data/flash_items.csv. Mỗi đơn hàng đọc FlashSaleItem, kiểm tra soldQty + quantity ≤ limitedQty, tăng soldQty, tăng version rồi ghi lại file. Khi 1.000 luồng cùng mua FSI-00167, toàn bộ luồng cùng truy cập một trạng thái chia sẻ duy nhất.")
doc.add_heading("1.2 BIG QUESTION", level=1)
callout("RESEARCH QUESTION",
        "Race condition nào xuất hiện khi nhiều thread cập nhật cùng một file CSV, và kỹ thuật đồng bộ nào giữ 0% âm kho đồng thời không làm throughput giảm quá 30%?",
        fill=LIGHT_BLUE, accent=BLUE)
doc.add_heading("1.3 Phạm vi", level=1)
bullet("Đồng thời trong một tiến trình Java, một JVM, trên Windows.")
bullet("Một flash-sale item, 1.000 thread, mỗi thread yêu cầu 1 đơn vị.")
bullet("Bốn cơ chế được triển khai sẵn: NO_LOCK, FILE_LOCK, SYNCHRONIZED, OPTIMISTIC.")
bullet("Ba lần lặp độc lập; báo cáo trung bình và độ lệch chuẩn.")

# System under test.
new_page("2. Hệ thống được kiểm thử", "Đường đi dữ liệu từ thread đến flash_items.csv", "04 · System under test")
doc.add_heading("2.1 Thành phần liên quan", level=1)
make_table(
    ["Thành phần", "Vai trò trong thí nghiệm"],
    [["SimulatorService", "Tạo 1.000 worker, đồng bộ điểm bắt đầu bằng CountDownLatch, đo tổng thời gian và TPS."],
     ["FlashSaleItemRepository", "Thực thi bốn chiến lược read–check–update–rewrite trên CSV."],
     ["SimulatorResult", "Tính lost update, oversold, violation rate và so sánh với baseline."],
     ["OrderTransactionRepository", "Ghi thời gian và trạng thái của từng yêu cầu để truy vết."],
     ["flash_items.csv", "Nguồn trạng thái kho; FSI-00167 có limitedQty=96."]],
    widths=[1.9, 4.7], font_size=8.8)
doc.add_heading("2.2 Mô hình trạng thái", level=1)
code_block([
    "Invariant vật lý: soldQty <= limitedQty",
    "Tồn kho ghi nhận: remainingRecorded = limitedQty - finalSoldQty",
    "Tồn kho logic: remainingLogical = limitedQty - successfulOrderQty",
    "Lost update: successfulOrderQty - finalSoldQty",
])
paragraph("Điểm quan trọng: file cuối có thể vẫn hiển thị soldQty ≤ limitedQty trong khi số đơn đã báo thành công lớn hơn tồn kho. Khi đó âm kho bị che khuất dưới dạng lost update; đây chính là trường hợp NO_LOCK của thí nghiệm.")

# Race timeline.
new_page("3. Race condition quan sát được", "Lost update biến 1.000 giao dịch thành công thành soldQty = 1", "05 · Race condition")
doc.add_heading("3.1 Interleaving tối giản", level=1)
make_table(
    ["Bước", "Thread A", "Thread B", "CSV"],
    [["1", "Đọc soldQty=0", "", "soldQty=0"],
     ["2", "", "Đọc soldQty=0", "soldQty=0"],
     ["3", "Kiểm tra 0+1≤96", "Kiểm tra 0+1≤96", "soldQty=0"],
     ["4", "Ghi soldQty=1", "", "soldQty=1"],
     ["5", "", "Ghi soldQty=1", "soldQty=1"],
     ["Kết quả", "2 đơn thành công", "", "Chỉ ghi nhận 1 đơn"]],
    widths=[.8, 1.8, 1.8, 2.1], font_size=8.8)
doc.add_heading("3.2 Khuếch đại ở 1.000 thread", level=1)
paragraph("Cả ba lần NO_LOCK đều báo 1.000/1.000 giao dịch thành công nhưng finalSoldQty chỉ bằng 1. Vì limitedQty=96, có 904 đơn thành công vượt quá tồn kho logic; negative-stock rate = 904/1.000 = 90,4%. Đồng thời 999/1.000 phép tăng không xuất hiện trong file cuối, tạo consistency violation rate = 99,9%.")
callout("KẾT LUẬN VỀ LỖI",
        "Race condition chính không phải file cuối có số âm trực tiếp; đó là lost update do read–modify–write và rewriteAll không nguyên tử. Âm kho xuất hiện ở sổ đơn hàng logic, còn file CSV bị ghi đè nên che giấu hậu quả.",
        fill=RED_LIGHT, accent=RED)

# Mechanisms.
new_page("4. Bốn cơ chế đồng bộ", "Cùng một nghiệp vụ, khác phạm vi khóa và chiến lược xung đột", "06 · Mechanisms")
make_table(
    ["Cơ chế", "Cách hoạt động", "Phạm vi", "Rủi ro chính"],
    [["NO_LOCK", "Đọc–kiểm tra–ghi trực tiếp", "Không có", "Lost update, file race"],
     ["FILE_LOCK", "Khóa độc quyền toàn file bằng NIO", "Hệ điều hành / JVM", "Overlapping lock trong cùng JVM"],
     ["SYNCHRONIZED", "Khóa monitor trên method repository", "Một instance trong JVM", "Tuần tự hóa, không cross-process"],
     ["OPTIMISTIC", "Đọc version, CAS trong critical section, retry tối đa 3", "Entity + JVM", "Retry storm khi contention cao"]],
    widths=[1.25, 2.3, 1.25, 1.85], font_size=8.2)
paragraph("SYNCHRONIZED tạo quan hệ happens-before giữa lần nhả và lần lấy cùng monitor, bảo đảm thread sau nhìn thấy cập nhật của thread trước [R1][R4]. OPTIMISTIC chỉ commit khi version đọc vẫn bằng version hiện tại; xung đột sẽ retry. FILE_LOCK giữ tính đúng của lần ghi thành công nhưng API này được thiết kế cho phối hợp giữa chương trình/tiến trình, không phải nhiều thread trong cùng JVM [R2].")
doc.add_heading("4.1 Baseline", level=1)
paragraph("Báo cáo dùng SYNCHRONIZED làm baseline vì SimulatorResult.BASELINE_MECHANISM trong source hiện tại đặt bằng SYNCHRONIZED và SimulatorView cũng mô tả đây là baseline an toàn. Chuỗi mô tả enum NO_LOCK vẫn chứa chữ “Baseline”; đây là nhãn cũ nên cần sửa để tránh hiểu nhầm.")

# Hypotheses.
new_page("5. Giả thuyết nghiên cứu", "Các dự đoán được kiểm tra bằng cùng một workload", "07 · Hypotheses")
make_table(
    ["Mã", "Giả thuyết", "Kỳ vọng"],
    [["H1", "NO_LOCK cho TPS cao nhất nhưng vi phạm nhất quán.", "Chấp nhận"],
     ["H2", "SYNCHRONIZED giữ 0% âm kho và dùng đủ tồn kho.", "Chấp nhận"],
     ["H3", "OPTIMISTIC an toàn nhưng retry làm giảm TPS ở contention cực cao.", "Chấp nhận"],
     ["H4", "FILE_LOCK an toàn nhưng phù hợp kém cho 1.000 thread cùng JVM.", "Chấp nhận"],
     ["H5", "Ít nhất một cơ chế an toàn giữ TPS ≥70% baseline.", "Chấp nhận"]],
    widths=[.55, 4.9, 1.1], font_size=8.7)
doc.add_heading("5.1 Tiêu chí quyết định", level=1)
numbered("Tỷ lệ âm kho logic bằng 0% ở cả ba lần lặp.")
numbered("Tỷ lệ sai lệch nhất quán bằng 0% ở cả ba lần lặp (guardrail bổ sung).")
numbered("TPS trung bình không thấp hơn 70% TPS trung bình của SYNCHRONIZED, tức tối thiểu 34,9 TPS.")
paragraph("Guardrail nhất quán không thay đổi kết luận nhưng ngăn một cơ chế “đạt” chỉ vì file cuối không âm trong khi vẫn mất cập nhật.")

# Experiment design.
new_page("6. Thiết kế thực nghiệm", "12 lượt đo độc lập, không sửa dữ liệu thật", "08 · Experimental design")
make_table(
    ["Thuộc tính", "Giá trị"],
    [["Item", "FSI-00167"], ["Tồn kho giới hạn", "96"], ["Threads", "1.000"],
     ["Số lượng/thread", "1"], ["Cơ chế", "4"], ["Lặp", "3"],
     ["Tổng lượt đo", "12"], ["Baseline", "SYNCHRONIZED"],
     ["JVM", "-Xms256m -Xmx1536m -Xss256k"]],
    widths=[2.2, 4.3], font_size=9)
doc.add_heading("6.1 Kiểm soát dữ liệu", level=1)
paragraph("Mỗi lần lặp sao chép data/flash_items.csv và data/customers.csv sang reports/experiment/results/run-XX. Simulator chạy trên bản sao, reset soldQty trước từng cơ chế, ghi transaction và summary vào cùng thư mục run. Vì vậy dữ liệu ứng dụng gốc không bị thay đổi.")
doc.add_heading("6.2 Điều phối thread", level=1)
paragraph("SimulatorService dùng fixed thread pool 1.000 worker. readyGate bảo đảm các task đã sẵn sàng, startGate phát lệnh bắt đầu đồng loạt, và doneGate chờ hoàn tất. Tổng thời gian đo từ ngay trước startGate.countDown() đến sau khi doneGate về 0.")
callout("LƯU Ý THIẾT KẾ", "Mỗi lần lặp chạy trong một JVM mới để giảm ảnh hưởng trạng thái còn sót. Thứ tự cơ chế vẫn cố định theo enum; đây là một threat to validity được báo cáo ở phần 17.", fill="FFFAEB", accent="B54708")

# Metrics.
new_page("7. Định nghĩa metric", "Phân biệt âm kho logic, oversold ghi nhận và lost update", "09 · Metrics")
code_block([
    "successfulQty = successCount × quantityPerThread",
    "logicalNegativeQty = max(0, successfulQty − limitedQty)",
    "negativeStockRate = logicalNegativeQty / successfulQty × 100%",
    "lostUpdateQty = max(0, successfulQty − finalSoldQty)",
    "consistencyViolationRate = (lostUpdateQty + explicitOversoldQty) / successfulQty × 100%",
    "throughput = successCount / (totalTimeMs / 1000)",
    "vsSync = (meanTPS − meanTPSSync) / meanTPSSync × 100%",
])
doc.add_heading("7.1 Vì sao không chỉ nhìn oversoldQty", level=1)
paragraph("Trong source, explicitOversoldQty = max(0, finalSoldQty − limitedQty). NO_LOCK luôn cho giá trị này bằng 0 vì nhiều thread cùng ghi soldQty=1. Nếu chỉ nhìn cột này, ta sẽ kết luận sai rằng không âm kho. Do đó báo cáo thêm logicalNegativeQty dựa trên số giao dịch đã báo thành công.")
doc.add_heading("7.2 Throughput và fail-fast", level=1)
paragraph("TPS hiện tại chỉ đếm giao dịch thành công. Một cơ chế thất bại rất nhanh có thể có tổng thời gian nhỏ, làm TPS trông cao dù bỏ lại tồn kho. Vì vậy báo cáo luôn đọc TPS cùng successCount và inventory utilization.")

# Raw results.
new_page("8. Kết quả thô của 12 lượt đo", "Mỗi hàng là một cơ chế trong một lần lặp độc lập", "10 · Raw results")
raw_table = []
for row in raw_rows:
    raw_table.append([
        row["repeat"], row["mechanism"], row["successCount"], row["finalSoldQty"],
        row["lostUpdateQty"], f"{float(row['totalTimeMs']):.1f}",
        f"{float(row['throughputTps']):.1f}", f"{float(row['negativeStockRatePct']):.1f}%"
    ])
make_table(["Lần", "Cơ chế", "OK", "Sold", "Lost", "Time ms", "TPS", "Âm kho"],
           raw_table, widths=[.42, 1.25, .55, .55, .58, .78, .72, .72], font_size=7.2)
paragraph("Dữ liệu thô đầy đủ, bao gồm fail count, latency và consistency violation, nằm trong reports/experiment/raw_results_1000_threads_4_mechanisms_3_repeats.csv.")

# Aggregate results.
new_page("9. Tổng hợp kết quả", "Mean ± sample SD trên ba lần lặp", "11 · Aggregated results")
agg_rows = []
for name in ("NO_LOCK", "FILE_LOCK", "SYNCHRONIZED", "OPTIMISTIC"):
    row = summary[name]
    agg_rows.append([
        name,
        f"{float(row['meanThroughputTps']):.1f} ± {float(row['sdThroughputTps']):.1f}",
        f"{float(row['meanTotalTimeMs']):.1f} ± {float(row['sdTotalTimeMs']):.1f}",
        f"{float(row['meanNegativeStockRatePct']):.1f}%",
        f"{float(row['meanConsistencyViolationRatePct']):.1f}%",
        f"{float(row['inventoryUtilizationPct']):.1f}%",
        f"{float(row['vsSynchronizedMeanPct']):+.1f}%",
        "ĐẠT" if row["rqPass"] == "true" else "KHÔNG",
    ])
make_table(["Cơ chế", "TPS", "Total time ms", "Âm kho", "Sai lệch", "Dùng kho", "vs SYNC", "RQ"],
           agg_rows, widths=[1.05, 1.05, 1.15, .65, .72, .7, .72, .55], font_size=7.0)
doc.add_heading("9.1 Những con số quyết định", level=1)
bullet("NO_LOCK: 1.358,3 TPS nhưng 90,4% âm kho logic và 99,9% sai lệch.")
bullet("FILE_LOCK: 160,8 TPS, 0% âm kho, nhưng chỉ sử dụng 60,8% tồn kho.")
bullet("SYNCHRONIZED: 49,9 TPS, 0% âm kho, sử dụng 100% tồn kho.")
bullet("OPTIMISTIC: 39,8 TPS, 0% âm kho, sử dụng 100% tồn kho; chậm hơn baseline 20,3%.")

# Main chart.
new_page("10. Throughput so với tỷ lệ âm kho", "Biểu đồ trung tâm trả lời BIG QUESTION", "12 · Primary chart")
add_figure(FIG_DIR / "throughput_vs_negative_stock_rate.png",
           "Hình 1. Throughput trung bình vs. tỷ lệ âm kho logic; trục TPS theo thang log.", width=6.9)
paragraph("Vùng đạt mục tiêu nằm trên đường negative-stock rate = 0% và bên phải ngưỡng 34,9 TPS. FILE_LOCK, SYNCHRONIZED và OPTIMISTIC đều nằm trong vùng này. NO_LOCK đứng xa về phía throughput nhưng ở mức âm kho 90,4%, nên không thể xem là phương án hợp lệ.")

# Variability chart.
new_page("11. Độ biến thiên qua ba lần lặp", "Mean không che giấu mức dao động của từng cơ chế", "13 · Variability")
add_figure(FIG_DIR / "throughput_by_repeat.png",
           "Hình 2. TPS của từng cơ chế ở ba lần lặp; trục dọc theo thang log.", width=6.9)
paragraph("NO_LOCK có SD=468,2 TPS, tương đương hệ số biến thiên khoảng 34,5%. FILE_LOCK cũng dao động đáng kể do số lần giành được file lock thay đổi. SYNCHRONIZED ổn định nhất với SD=5,8 TPS; OPTIMISTIC có SD=7,2 TPS và giảm mạnh ở lần 3 do retry và contention kéo dài.")

# Integrity chart.
new_page("12. An toàn dữ liệu không đồng nghĩa chất lượng xử lý", "FILE_LOCK là ví dụ điển hình của metric bị đánh lừa bởi fail-fast", "14 · Integrity & utilization")
add_figure(FIG_DIR / "integrity_and_inventory_utilization.png",
           "Hình 3. Sai lệch nhất quán và mức sử dụng tồn kho trung bình.", width=6.9)
paragraph("Ba cơ chế khóa đều giữ consistency violation = 0%. Tuy nhiên FILE_LOCK chỉ chốt trung bình 58,3 đơn vị, để lại 37,7/96 đơn vị chưa bán. SYNCHRONIZED và OPTIMISTIC đều bán đủ 96 đơn vị, sau đó 904 yêu cầu còn lại thất bại đúng với lý do hết hàng.")

# No lock analysis.
new_page("13. Phân tích NO_LOCK", "Nhanh nhất vì bỏ toàn bộ chi phí an toàn—and bỏ luôn tính đúng", "15 · NO_LOCK")
doc.add_heading("13.1 Kết quả", level=1)
make_table(["Metric", "Giá trị TB"],
           [["TPS", "1.358,3"], ["Total time", "788,5 ms"], ["Success", "1.000/1.000"],
            ["Final soldQty", "1"], ["Lost update", "999"], ["Âm kho logic", "904 (90,4%)"]],
           widths=[2.3, 4.2], font_size=9)
doc.add_heading("13.2 Nguyên nhân", level=1)
paragraph("sellNoLock gọi findById, kiểm tra coBanDuoc, tăng soldQty trên object cục bộ rồi update. update lại đọc toàn bộ CSV và rewriteAll. Không có monitor, version check hay file lock bao quanh chuỗi thao tác, nên hàng trăm thread cùng dựa trên snapshot soldQty=0 và ghi đè lẫn nhau.")
callout("PHÁN QUYẾT", "NO_LOCK chỉ nên dùng để minh họa race condition hoặc làm negative control. Không dùng làm baseline quyết định và tuyệt đối không dùng trong luồng đặt hàng thật.", fill=RED_LIGHT, accent=RED)

# File lock analysis.
new_page("14. Phân tích FILE_LOCK", "0% âm kho nhưng API không phù hợp để khóa nhiều thread trong cùng JVM", "16 · FILE_LOCK")
doc.add_heading("14.1 Kết quả", level=1)
paragraph("FILE_LOCK đạt 160,8 ± 45,3 TPS và không mất cập nhật ở các giao dịch thành công. Tuy nhiên successCount chỉ 43, 73 và 59; mức sử dụng tồn kho trung bình 60,8%. Các transaction thất bại có errorMessage rỗng vì OverlappingFileLockException không có message mặc định, phù hợp với hành vi API được Oracle mô tả [R2][R3].")
doc.add_heading("14.2 Ý nghĩa", level=1)
paragraph("FileLock được nắm thay mặt toàn JVM và không phù hợp để điều phối nhiều thread trong cùng JVM [R2]. Khi một thread đã giữ lock hoặc thread khác đang chờ vùng chồng lấn, lần gọi khác có thể ném unchecked exception ngay. Do simulator bắt exception và kết thúc yêu cầu, tổng thời gian ngắn làm TPS trông tốt.")
doc.add_heading("14.3 Cách cải thiện", level=1)
bullet("Đặt một JVM mutex (synchronized/ReentrantLock) bên ngoài FileLock để tuần tự hóa thread nội bộ.")
bullet("Giữ FileLock ở lớp trong để bảo vệ cross-process.")
bullet("Bắt OverlappingFileLockException, retry có backoff và giới hạn thời gian.")
callout("PHÁN QUYẾT", "Đạt tiêu chí RQ hẹp, nhưng không được khuyến nghị ở trạng thái triển khai hiện tại vì bỏ đơn hợp lệ và bỏ lại tồn kho.", fill="FFFAEB", accent="B54708")

# Sync analysis.
new_page("15. SYNCHRONIZED", "Mặc định phù hợp nhất cho kiến trúc một JVM hiện tại", "17 · SYNCHRONIZED")
doc.add_heading("15.1 Kết quả", level=1)
paragraph("Cả ba lần đều có đúng 96 thành công, 904 thất bại do hết hàng, finalSoldQty=96, lostUpdate=0 và âm kho=0. Throughput trung bình 49,9 ± 5,8 TPS. Đây là cơ chế ổn định nhất trong nhóm an toàn.")
doc.add_heading("15.2 Vì sao đúng", level=1)
paragraph("Khai báo synchronized trên sellWithSynchronized khiến mỗi lần chỉ một thread được chạy toàn bộ read–check–update. Khi thread rời monitor, các ghi trước đó happens-before lần thread tiếp theo vào cùng monitor, nên snapshot không bị cũ [R1][R4].")
doc.add_heading("15.3 Giới hạn", level=1)
bullet("Chỉ bảo vệ các thread đi qua cùng instance repository trong cùng JVM.")
bullet("Không chặn một tiến trình Java khác hoặc ứng dụng ngoài sửa file.")
bullet("Critical section chứa đọc và rewrite toàn file nên chi phí tăng theo kích thước CSV.")
callout("PHÁN QUYẾT", "Khuyến nghị cho bản hiện tại: đơn giản, đúng, bán đủ tồn kho và đáp ứng ngưỡng hiệu năng theo chính baseline an toàn.", fill=GREEN_LIGHT, accent=GREEN)

# Optimistic analysis.
new_page("16. Phân tích OPTIMISTIC", "An toàn và đạt ngưỡng, nhưng contention cực cao tạo retry storm", "18 · OPTIMISTIC")
doc.add_heading("16.1 Kết quả", level=1)
paragraph("OPTIMISTIC bán đủ 96/96 đơn vị ở cả ba lần, không lost update và không âm kho. TPS trung bình 39,8 ± 7,2, thấp hơn SYNCHRONIZED 20,3% nên vẫn nằm trong giới hạn giảm 30%.")
doc.add_heading("16.2 Cơ chế", level=1)
numbered("Thread đọc item và ghi nhớ versionDoc.")
numbered("Thread kiểm tra tồn kho và tạo bản updated với version+1.")
numbered("Trong optimisticWriteLock, repository đọc lại file; version khớp thì rewrite và commit.")
numbered("Version đổi thì retry, tối đa 3 lần.")
paragraph("Trong workload 1.000 thread cùng một item, gần như mọi thread đọc cùng version ban đầu. Diagnostic log ghi hàng trăm xung đột ở lần retry 1 và 2. Đây là workload xấu nhất cho optimistic locking; ưu điểm của cơ chế này sẽ rõ hơn khi xung đột phân tán trên nhiều item.")
callout("PHÁN QUYẾT", "Đạt RQ và là phương án thứ hai. Nên dùng khi contention thực tế thấp hoặc khi mở rộng theo từng item; với hotspot duy nhất, SYNCHRONIZED đơn giản và nhanh hơn.", fill=LIGHT_BLUE, accent=PURPLE)

# Decision matrix.
new_page("17. Trả lời Research Question", "Tách kết quả định lượng khỏi quyết định kỹ thuật", "19 · RQ decision")
make_table(
    ["Cơ chế", "0% âm kho", "TPS ≥34,9", "Dùng đủ kho", "RQ hẹp", "Khuyến nghị"],
    [["NO_LOCK", "Không", "Có", "Không", "KHÔNG", "Không dùng"],
     ["FILE_LOCK", "Có", "Có", "Không", "ĐẠT", "Cần sửa hybrid"],
     ["SYNCHRONIZED", "Có", "Có", "Có", "ĐẠT", "Ưu tiên"],
     ["OPTIMISTIC", "Có", "Có", "Có", "ĐẠT", "Phương án 2"]],
    widths=[1.15, .85, .9, .9, .75, 1.45], font_size=8.2)
callout("ANSWER TO BIG QUESTION",
        "Race condition là lost update trong chuỗi read–check–rewrite, làm 1.000 đơn cùng báo thành công nhưng CSV chỉ ghi nhận một lần tăng. Ba cơ chế FILE_LOCK, SYNCHRONIZED và OPTIMISTIC đều ngăn âm kho và đạt ngưỡng throughput theo baseline SYNCHRONIZED; tuy nhiên SYNCHRONIZED là lựa chọn triển khai tốt nhất cho một JVM, OPTIMISTIC đứng thứ hai, còn FILE_LOCK cần kết hợp JVM mutex/retry trước khi dùng.",
        fill=GREEN_LIGHT, accent=GREEN)
paragraph("Câu trả lời “ba cơ chế đạt” là kết luận thống kê theo tiêu chí được hỏi. Câu trả lời “chọn SYNCHRONIZED” là quyết định kiến trúc sau khi bổ sung tiêu chí sử dụng tồn kho và xử lý lỗi.")

# Baseline sensitivity.
new_page("18. Phân tích độ nhạy của baseline", "Vì sao không dùng NO_LOCK làm chuẩn chính", "20 · Baseline sensitivity")
sync_tps = float(summary["SYNCHRONIZED"]["meanThroughputTps"])
no_tps = float(summary["NO_LOCK"]["meanThroughputTps"])
sensitivity = []
for name in ("FILE_LOCK", "SYNCHRONIZED", "OPTIMISTIC"):
    tps = float(summary[name]["meanThroughputTps"])
    sensitivity.append([name, f"{tps:.1f}", f"{(tps-no_tps)*100/no_tps:+.1f}%", "Không" if (tps-no_tps)*100/no_tps < -30 else "Có"])
make_table(["Cơ chế an toàn", "TPS", "vs NO_LOCK", "Đạt -30%?"], sensitivity,
           widths=[2.1, 1.1, 1.2, 1.6], font_size=9)
paragraph("Nếu lấy NO_LOCK làm baseline, không cơ chế an toàn nào đạt ngưỡng -30%: FILE_LOCK giảm khoảng 88,2%, SYNCHRONIZED giảm 96,3%, OPTIMISTIC giảm 97,1%. Nhưng NO_LOCK tạo trạng thái sai, nên tốc độ của nó không đại diện cho công việc hoàn thành đúng.")
callout("NGUYÊN TẮC BASELINE", "Baseline để ra quyết định phải là triển khai tối thiểu nhưng hợp lệ. NO_LOCK nên được giữ như negative control để đo giá phải trả của race condition, không phải mốc chấp nhận hiệu năng.", fill=LIGHT_BLUE, accent=BLUE)
paragraph("Do source đã đặt SYNCHRONIZED làm BASELINE_MECHANISM, báo cáo giữ nguyên lựa chọn này. Nên sửa mô tả enum NO_LOCK từ “Không khóa (Baseline)” thành “Không khóa (Negative control)” để thống nhất thông điệp.")

# Threats.
new_page("19. Threats to validity", "Giới hạn cần nêu rõ trước khi khái quát kết quả", "21 · Validity")
make_table(
    ["Loại", "Nguy cơ", "Ảnh hưởng / giảm thiểu"],
    [["Internal", "Thứ tự cơ chế cố định", "JIT/cache có thể thiên lệch; mỗi repeat dùng JVM mới nhưng chưa randomize order."],
     ["Internal", "Chỉ 3 lần lặp", "SD chỉ mô tả sơ bộ; cần ≥10–30 lần để suy luận chắc hơn."],
     ["Construct", "TPS chỉ đếm success", "Fail-fast có thể tăng TPS biểu kiến; báo cáo kèm utilization và success count."],
     ["Construct", "Âm kho bị che bởi lost update", "Bổ sung logicalNegativeQty thay vì chỉ nhìn finalSoldQty."],
     ["External", "Một item hotspot", "Kết quả optimistic có thể tốt hơn khi đơn phân tán trên nhiều item."],
     ["External", "Một JVM/Windows", "FileLock phụ thuộc hệ điều hành và khác với multi-process thực sự."],
     ["External", "CSV 409 item", "Thời gian rewrite tăng theo kích thước file; không suy rộng trực tiếp sang database."]],
    widths=[.75, 2.05, 3.55], font_size=7.7)
paragraph("Do các giới hạn trên, kết luận phù hợp nhất là: với source, dữ liệu và máy chạy hiện tại, SYNCHRONIZED là mặc định an toàn và đầy đủ nhất. Đây không phải tuyên bố rằng synchronized luôn tối ưu cho mọi hệ thống lưu trữ.")

# Recommendations.
new_page("20. Khuyến nghị kỹ thuật", "Từ bài lab đến triển khai có thể bảo vệ cả thread và process", "22 · Recommendations")
doc.add_heading("20.1 Ngắn hạn", level=1)
numbered("Dùng SYNCHRONIZED cho luồng đặt hàng mặc định trong một JVM.")
numbered("Đổi nhãn NO_LOCK thành Negative control và chặn cơ chế này khỏi menu nghiệp vụ thật.")
numbered("Ghi tên exception thay vì chỉ message để OverlappingFileLockException không bị rỗng trong transactions.csv.")
numbered("Bổ sung success rate, inventory utilization và error-class breakdown vào simulation_results.csv.")
doc.add_heading("20.2 Trung hạn", level=1)
numbered("Nếu cần cross-process: dùng hybrid lock — JVM mutex bên ngoài và FileLock bên trong.")
numbered("Nếu dùng OPTIMISTIC: retry có exponential backoff + jitter; cân nhắc MAX_RETRY theo SLA.")
numbered("Thay rewrite toàn file bằng atomic temp-file + move và khóa theo item khi kiến trúc cho phép.")
doc.add_heading("20.3 Dài hạn", level=1)
paragraph("Khi tải thật vượt phạm vi bài lab, chuyển tồn kho sang database hỗ trợ transaction/row lock hoặc atomic conditional update. CSV phù hợp cho minh họa race condition và persistence nhỏ, nhưng không phải nền tảng xử lý hotspot production.")

# Reproducibility.
new_page("21. Tái lập thí nghiệm", "Mọi dữ liệu và script đều được lưu cùng báo cáo", "23 · Reproducibility")
doc.add_heading("21.1 Tệp đầu vào và kết quả", level=1)
make_table(["Tệp", "Mục đích"],
           [["reports/experiment/ExperimentRunner.java", "Runner cô lập, tạo bản sao CSV theo repeat."],
            ["reports/experiment/results/run-01..03", "Transactions, diagnostics và kết quả từng repeat."],
            ["reports/experiment/raw_results_...csv", "12 dòng dữ liệu thô hợp nhất."],
            ["reports/experiment/aggregated_results.csv", "Mean, SD, utilization và kết luận RQ."],
            ["reports/experiment/analyze_results.py", "Tổng hợp thống kê tái lập."],
            ["reports/experiment/generate_charts.mjs", "Sinh ba biểu đồ bằng SVG/Node."]],
           widths=[3.0, 3.45], font_size=8.2)
doc.add_heading("21.2 Lệnh chạy", level=1)
code_block([
    "javac -encoding UTF-8 -cp target/classes -d reports/experiment/bin reports/experiment/ExperimentRunner.java",
    "java -Xms256m -Xmx1536m -Xss256k -cp \"target/classes;reports/experiment/bin\" ExperimentRunner 1 reports/experiment/results",
    "# Lặp lại với repeat 2 và 3, sau đó chạy analyze_results.py",
])
paragraph("Mỗi run có bản sao flash_items.csv và customers.csv, nên có thể kiểm tra finalSoldQty và transaction-level error mà không đụng vào data gốc.")

# Conclusion.
new_page("22. Kết luận", "An toàn phải được đo cùng throughput—và throughput phải được đọc cùng chất lượng hoàn thành", "24 · Conclusion")
paragraph("Thí nghiệm 12 lượt cho thấy NO_LOCK đạt throughput thô cao nhất nhưng phá vỡ tính đúng: 999 lost updates và 90,4% âm kho logic. Đây là bằng chứng trực tiếp rằng thao tác đọc–kiểm tra–rewrite trên CSV không thể được xem là một transaction nguyên tử.")
paragraph("FILE_LOCK, SYNCHRONIZED và OPTIMISTIC đều giữ 0% âm kho và vượt ngưỡng 34,9 TPS. Tuy nhiên FILE_LOCK chỉ sử dụng 60,8% tồn kho do các lần khóa chồng lấn trong cùng JVM thất bại nhanh. SYNCHRONIZED và OPTIMISTIC đều bán đủ 96 đơn vị; SYNCHRONIZED nhanh hơn, ổn định hơn và đơn giản hơn cho kiến trúc hiện tại.")
callout("KẾT LUẬN CUỐI CÙNG",
        "Chọn SYNCHRONIZED làm cơ chế mặc định cho ứng dụng CSV một JVM. Giữ OPTIMISTIC như lựa chọn thay thế khi contention phân tán. Chỉ dùng FILE_LOCK sau khi bổ sung khóa nội bộ/retry, và giữ NO_LOCK như negative control.",
        fill=GREEN_LIGHT, accent=GREEN)
paragraph("Research Question được trả lời: lost update là race condition chủ đạo; các cơ chế đồng bộ ngăn được âm kho mà không giảm quá 30% so với baseline an toàn là FILE_LOCK, SYNCHRONIZED và OPTIMISTIC—trong đó SYNCHRONIZED là lựa chọn triển khai được khuyến nghị.")

# References.
new_page("Tài liệu tham khảo", "Nguồn code, dữ liệu thực nghiệm và tài liệu Java chính thức", "25 · References")
refs = [
    ("[S1]", "src/main/java/service/SimulatorService.java — điều phối thread, đo TPS, ghi summary."),
    ("[S2]", "src/main/java/service/SimulatorResult.java — baseline, lost update, violation rate."),
    ("[S3]", "src/main/java/repository/FlashSaleItemRepository.java — bốn cơ chế cập nhật CSV."),
    ("[S4]", "src/main/java/model/FlashSaleItem.java — invariant soldQty ≤ limitedQty và version."),
    ("[D1]", "reports/experiment/raw_results_1000_threads_4_mechanisms_3_repeats.csv — dữ liệu thô."),
    ("[D2]", "reports/experiment/aggregated_results.csv — kết quả trung bình và độ lệch chuẩn."),
]
for code, value in refs:
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(5)
    r = p.add_run(f"{code} ")
    set_run_font(r, bold=True, color=BLUE)
    p.add_run(value)

p = doc.add_paragraph()
r = p.add_run("[R1] ")
set_run_font(r, bold=True, color=BLUE)
add_hyperlink(p, "Java Language Specification SE 8, Chapter 17 — Threads and Locks",
              "https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html")
p = doc.add_paragraph()
r = p.add_run("[R2] ")
set_run_font(r, bold=True, color=BLUE)
add_hyperlink(p, "Oracle Java SE 8 API — FileLock",
              "https://docs.oracle.com/javase/8/docs/api/java/nio/channels/FileLock.html")
p = doc.add_paragraph()
r = p.add_run("[R3] ")
set_run_font(r, bold=True, color=BLUE)
add_hyperlink(p, "Oracle Java API — OverlappingFileLockException",
              "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/channels/OverlappingFileLockException.html")
p = doc.add_paragraph()
r = p.add_run("[R4] ")
set_run_font(r, bold=True, color=BLUE)
add_hyperlink(p, "Oracle Java SE 8 API — java.util.concurrent memory consistency properties",
              "https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html")

REPORT_DIR.mkdir(parents=True, exist_ok=True)
doc.save(OUT)
print(OUT)
