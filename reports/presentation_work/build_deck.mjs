import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.join(HERE, "output");
const PPTX = path.join(HERE, "..", "FlashSale_Concurrency_Experiment_Slides.pptx");
const FIG = path.join(HERE, "..", "experiment", "figures");

const C = {
  ink: "#111827",
  muted: "#667085",
  faint: "#98A2B3",
  line: "#D0D5DD",
  panel: "#F2F4F7",
  blue: "#155EEF",
  blueSoft: "#EFF4FF",
  red: "#D92D20",
  redSoft: "#FEF3F2",
  teal: "#0E9384",
  tealSoft: "#F0FDF9",
  purple: "#7A5AF8",
  purpleSoft: "#F4F3FF",
  amber: "#DC6803",
  amberSoft: "#FFFAEB",
  white: "#FFFFFF",
  black: "#000000",
};

const mechanismColors = {
  NO_LOCK: C.red,
  FILE_LOCK: C.teal,
  SYNCHRONIZED: C.blue,
  OPTIMISTIC: C.purple,
};

const presentation = Presentation.create({ slideSize: { width: 1280, height: 720 } });

function shape(slide, geometry, left, top, width, height, fill = C.white, lineFill = "none", lineWidth = 0, radius) {
  const s = slide.shapes.add({
    geometry,
    position: { left, top, width, height },
    fill,
    line: { style: "solid", fill: lineFill, width: lineWidth },
    ...(radius ? { borderRadius: radius } : {}),
  });
  return s;
}

function text(slide, value, left, top, width, height, opts = {}) {
  const t = shape(slide, "textbox", left, top, width, height, "none", "none", 0);
  t.text = String(value);
  t.text.style = {
    fontSize: opts.size ?? 22,
    typeface: "Helvetica Neue",
    color: opts.color ?? C.ink,
    bold: opts.bold ?? false,
    alignment: opts.align ?? "left",
    verticalAlignment: opts.valign ?? "top",
    autoFit: opts.autoFit ?? "shrinkText",
    ...(opts.italic ? { italic: true } : {}),
  };
  t.text.insets = opts.insets ?? { top: 0, right: 0, bottom: 0, left: 0 };
  return t;
}

function baseSlide(eyebrow, title, subtitle = "") {
  const slide = presentation.slides.add();
  slide.background.fill = C.white;
  shape(slide, "rect", 41, 26, 52, 5, C.blue);
  text(slide, eyebrow.toUpperCase(), 41, 42, 500, 24, { size: 12, bold: true, color: C.blue });
  text(slide, title, 41, 70, 1198, 62, { size: 36, bold: true });
  if (subtitle) text(slide, subtitle, 41, 132, 1198, 38, { size: 17, color: C.muted });
  const n = presentation.slides.items.length;
  text(slide, "LAB211 · CSV CONCURRENCY EXPERIMENT", 41, 674, 420, 18, { size: 10, color: C.faint });
  text(slide, String(n).padStart(2, "0"), 1184, 670, 54, 22, { size: 12, color: C.muted, align: "right" });
  return slide;
}

function panel(slide, left, top, width, height, fill = C.panel, border = "none") {
  return shape(slide, "roundRect", left, top, width, height, fill, border, border === "none" ? 0 : 1, "rounded-xl");
}

function label(slide, value, left, top, width, color = C.blue, fill = C.blueSoft) {
  panel(slide, left, top, width, 30, fill);
  text(slide, value, left + 10, top + 6, width - 20, 18, { size: 12, bold: true, color, align: "center" });
}

function bullet(slide, value, left, top, width, color = C.ink, size = 20) {
  shape(slide, "ellipse", left, top + 9, 7, 7, C.blue);
  text(slide, value, left + 19, top, width - 19, 46, { size, color });
}

function statCard(slide, left, top, width, value, caption, color = C.blue, fill = C.panel) {
  panel(slide, left, top, width, 125, fill);
  text(slide, value, left + 18, top + 17, width - 36, 50, { size: 34, bold: true, color });
  text(slide, caption, left + 18, top + 76, width - 36, 34, { size: 15, color: C.muted });
}

function mechanismCard(slide, left, top, width, name, tps, neg, util, decision) {
  const color = mechanismColors[name];
  panel(slide, left, top, width, 346, C.white, C.line);
  shape(slide, "rect", left, top, width, 7, color);
  text(slide, name, left + 18, top + 22, width - 36, 32, { size: 20, bold: true, color });
  text(slide, `${tps} TPS`, left + 18, top + 74, width - 36, 36, { size: 29, bold: true });
  text(slide, "THROUGHPUT TRUNG BÌNH", left + 18, top + 112, width - 36, 20, { size: 11, bold: true, color: C.muted });
  text(slide, `${neg}%`, left + 18, top + 158, width - 36, 32, { size: 25, bold: true, color: neg === "0.0" ? C.teal : C.red });
  text(slide, "TỶ LỆ ÂM KHO LOGIC", left + 18, top + 194, width - 36, 20, { size: 11, bold: true, color: C.muted });
  text(slide, `${util}%`, left + 18, top + 231, width - 36, 28, { size: 22, bold: true });
  text(slide, "KHAI THÁC KHO", left + 18, top + 265, width - 36, 20, { size: 11, bold: true, color: C.muted });
  label(slide, decision, left + 18, top + 300, width - 36, decision === "FAIL" ? C.red : C.teal, decision === "FAIL" ? C.redSoft : C.tealSoft);
}

function manualTable(slide, left, top, widths, rows, opts = {}) {
  const rowH = opts.rowH ?? 34;
  const headerFill = opts.headerFill ?? C.ink;
  const headerColor = opts.headerColor ?? C.white;
  for (let r = 0; r < rows.length; r++) {
    let x = left;
    for (let c = 0; c < widths.length; c++) {
      const fill = r === 0 ? headerFill : (r % 2 === 0 ? C.panel : C.white);
      shape(slide, "rect", x, top + r * rowH, widths[c], rowH, fill, C.line, 0.7);
      const cell = rows[r][c] ?? "";
      const isMechanism = c === (opts.mechanismCol ?? -1) && r > 0;
      const color = isMechanism ? (mechanismColors[cell] ?? C.ink) : (r === 0 ? headerColor : C.ink);
      text(slide, cell, x + 7, top + r * rowH + 7, widths[c] - 14, rowH - 12, {
        size: r === 0 ? (opts.headerSize ?? 12) : (opts.bodySize ?? 13),
        bold: r === 0 || isMechanism,
        color,
        align: c === 0 || isMechanism ? "left" : "center",
        valign: "middle",
      });
      x += widths[c];
    }
  }
}

async function addImage(slide, file, left, top, width, height, alt) {
  const bytes = await fs.readFile(file);
  const imageBytes = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
  const img = slide.images.add({
    blob: imageBytes,
    contentType: "image/png",
    fit: "contain",
    alt,
    position: { left, top, width, height },
  });
  return img;
}

function addArrow(slide, left, top, width = 72, color = C.blue) {
  shape(slide, "rect", left, top + 10, width - 22, 4, color);
  text(slide, "→", left + width - 34, top - 3, 34, 30, { size: 27, bold: true, color, align: "right" });
}

// 1 — Cover, adapted from Codex Grid layout 01.
{
  const s = presentation.slides.add();
  s.background.fill = C.white;
  shape(s, "rect", 41, 36, 58, 6, C.blue);
  text(s, "LAB211 · NHÓM 3", 41, 58, 360, 26, { size: 13, bold: true, color: C.blue });
  text(s, "ĐỒNG THỜI TRÊN CSV", 41, 150, 730, 72, { size: 49, bold: true });
  text(s, "Race condition nào làm âm kho — và cơ chế nào an toàn mà vẫn giữ throughput?", 41, 240, 760, 112, { size: 29, color: C.ink });
  panel(s, 880, 112, 318, 430, C.ink);
  text(s, "FULL\nEXPERIMENT", 912, 149, 254, 74, { size: 24, bold: true, color: C.white });
  text(s, "1,000", 912, 271, 254, 60, { size: 51, bold: true, color: C.white });
  text(s, "threads", 912, 334, 254, 28, { size: 18, color: C.line });
  text(s, "4 × 3", 912, 407, 254, 53, { size: 43, bold: true, color: C.white });
  text(s, "cơ chế × lần lặp", 912, 464, 254, 28, { size: 17, color: C.line });
  label(s, "KẾT QUẢ THỰC NGHIỆM", 41, 426, 238, C.blue, C.blueSoft);
  text(s, "FSI-00167 · limitedQty = 96 · 21/07/2026", 41, 485, 680, 32, { size: 18, color: C.muted });
  text(s, "Báo cáo Word 26 trang · Bộ slide 20 trang", 41, 530, 680, 32, { size: 18, color: C.muted });
  text(s, "01", 1184, 670, 54, 22, { size: 12, color: C.muted, align: "right" });
}

// 2 — Big question and answer.
{
  const s = baseSlide("Research question", "Câu hỏi lớn — câu trả lời ngắn");
  panel(s, 41, 190, 1198, 177, C.ink);
  text(s, "“Khi 1.000 luồng cùng cập nhật một file CSV, race condition nào xảy ra và cách nào ngăn âm kho mà throughput không giảm quá 30%?”", 73, 222, 1134, 112, { size: 27, bold: true, color: C.white, align: "center", valign: "middle" });
  statCard(s, 41, 411, 278, "Lost update", "Race condition chính", C.red, C.redSoft);
  statCard(s, 343, 411, 278, "3/4", "Cơ chế đạt tiêu chí hẹp", C.teal, C.tealSoft);
  statCard(s, 645, 411, 278, "−20.3%", "OPTIMISTIC so với baseline", C.purple, C.purpleSoft);
  statCard(s, 947, 411, 292, "SYNC", "Khuyến nghị triển khai", C.blue, C.blueSoft);
}

// 3 — Why this matters.
{
  const s = baseSlide("Problem", "Âm kho có thể bị che giấu bởi dữ liệu cuối file", "NO_LOCK không ghi số âm, nhưng vẫn chấp nhận nhiều đơn hơn kho cho phép.");
  statCard(s, 41, 206, 270, "1,000", "Đơn được báo thành công", C.red, C.redSoft);
  statCard(s, 335, 206, 270, "1", "soldQty cuối file", C.red, C.redSoft);
  statCard(s, 629, 206, 270, "999", "Cập nhật bị mất", C.red, C.redSoft);
  statCard(s, 923, 206, 316, "−904", "Tồn kho logic", C.red, C.redSoft);
  panel(s, 41, 376, 1198, 212, C.panel);
  text(s, "Vì sao?", 73, 410, 220, 36, { size: 26, bold: true });
  bullet(s, "Nhiều luồng cùng đọc soldQty = 0.", 73, 463, 1080);
  bullet(s, "Mỗi luồng tự tính 1 và ghi đè kết quả của luồng khác.", 73, 511, 1080);
  bullet(s, "Kiểm tra âm kho chỉ dựa vào giá trị cuối file sẽ cho kết luận sai.", 73, 559, 1080);
}

// 4 — SUT.
{
  const s = baseSlide("System under test", "Đường đi của một giao dịch trên hệ thống CSV");
  const boxes = [
    [41, "1.000 thread", "Mỗi thread mua 1 sản phẩm"],
    [288, "SimulatorService", "Chọn cơ chế đồng bộ"],
    [535, "Transaction", "Đọc → kiểm tra → cập nhật"],
    [782, "flash_items.csv", "soldQty + version"],
    [1029, "Kết quả", "OK / Fail / TPS / integrity"],
  ];
  for (let i = 0; i < boxes.length; i++) {
    const [x, title, body] = boxes[i];
    panel(s, x, 245, 210, 188, i === 3 ? C.blueSoft : C.panel, i === 3 ? C.blue : "none");
    text(s, String(i + 1).padStart(2, "0"), x + 18, 266, 52, 28, { size: 15, bold: true, color: C.blue });
    text(s, title, x + 18, 311, 174, 38, { size: 22, bold: true });
    text(s, body, x + 18, 363, 174, 50, { size: 15, color: C.muted });
    if (i < boxes.length - 1) addArrow(s, x + 210, 322, 36);
  }
  text(s, "Hotspot duy nhất: FSI-00167 · limitedQty = 96", 41, 495, 1198, 42, { size: 23, bold: true, color: C.blue, align: "center" });
  text(s, "Mỗi lần lặp chạy trong JVM mới và trên bản sao CSV riêng; dữ liệu gốc không bị sửa.", 41, 554, 1198, 32, { size: 17, color: C.muted, align: "center" });
}

// 5 — Mechanisms.
{
  const s = baseSlide("Independent variable", "Bốn cơ chế được đặt trong cùng một workload");
  const xs = [41, 343, 645, 947];
  const data = [
    ["NO_LOCK", "Không vùng tới hạn", "Đối chứng âm"],
    ["FILE_LOCK", "Khóa file hệ điều hành", "Khóa ngoài JVM"],
    ["SYNCHRONIZED", "Monitor trong JVM", "Baseline an toàn"],
    ["OPTIMISTIC", "Kiểm tra version", "Phát hiện xung đột"],
  ];
  data.forEach(([name, body, role], i) => {
    const color = mechanismColors[name];
    panel(s, xs[i], 226, 278, 288, C.white, C.line);
    shape(s, "rect", xs[i], 226, 278, 8, color);
    text(s, name, xs[i] + 20, 260, 238, 38, { size: 21, bold: true, color });
    text(s, body, xs[i] + 20, 326, 238, 52, { size: 22, bold: true });
    text(s, role, xs[i] + 20, 410, 238, 35, { size: 17, color: C.muted });
    label(s, i === 0 ? "UNSAFE CONTROL" : "CANDIDATE", xs[i] + 20, 462, 238, i === 0 ? C.red : color, i === 0 ? C.redSoft : C.panel);
  });
  text(s, "NO_LOCK dùng để bộc lộ race condition — không dùng làm baseline hiệu năng cho câu hỏi an toàn.", 41, 564, 1198, 34, { size: 19, bold: true, color: C.red, align: "center" });
}

// 6 — Hypotheses.
{
  const s = baseSlide("Hypotheses", "Giả thuyết trước khi chạy");
  const rows = [
    ["H1", "NO_LOCK sẽ có lost update và âm kho logic", "Kỳ vọng: FAIL"],
    ["H2", "Ba cơ chế có đồng bộ sẽ giữ toàn vẹn", "Kỳ vọng: 0% âm kho"],
    ["H3", "OPTIMISTIC giảm throughput khi hotspot cực cao", "Kỳ vọng: retry nhiều"],
    ["H4", "FILE_LOCK có thể không phù hợp nhiều thread cùng JVM", "Kỳ vọng: fail-fast"],
  ];
  rows.forEach((r, i) => {
    const y = 192 + i * 103;
    label(s, r[0], 41, y + 11, 72, C.blue, C.blueSoft);
    text(s, r[1], 139, y, 750, 54, { size: 22, bold: true });
    text(s, r[2], 928, y + 9, 311, 34, { size: 17, color: i === 0 ? C.red : C.muted, align: "right" });
    shape(s, "rect", 139, y + 66, 1100, 1, C.line);
  });
}

// 7 — Protocol.
{
  const s = baseSlide("Experimental design", "Thiết kế full experiment: 1.000 × 4 × 3", "Tổng cộng 12 quan sát độc lập.");
  statCard(s, 41, 205, 278, "1,000", "Platform threads / run", C.blue, C.blueSoft);
  statCard(s, 343, 205, 278, "4", "Cơ chế đồng bộ", C.teal, C.tealSoft);
  statCard(s, 645, 205, 278, "3", "Lần lặp / cơ chế", C.purple, C.purpleSoft);
  statCard(s, 947, 205, 292, "12", "Tổng số phép đo", C.ink, C.panel);
  panel(s, 41, 385, 1198, 194, C.panel);
  bullet(s, "Mỗi thread đặt quantity = 1 trên cùng FSI-00167.", 73, 420, 1100);
  bullet(s, "Mỗi lần lặp dùng JVM mới; heap 256–1536 MB; stack 256 KB.", 73, 468, 1100);
  bullet(s, "Mỗi cơ chế bắt đầu từ cùng limitedQty = 96 và soldQty = 0.", 73, 516, 1100);
}

// 8 — Metrics and threshold.
{
  const s = baseSlide("Decision rule", "Metric và ngưỡng trả lời Research Question");
  panel(s, 41, 190, 590, 383, C.panel);
  text(s, "Metric đo", 69, 219, 200, 34, { size: 24, bold: true });
  bullet(s, "TPS = success / total time", 69, 278, 520, C.ink, 20);
  bullet(s, "Âm kho logic = max(0, success − 96)", 69, 329, 520, C.ink, 20);
  bullet(s, "Lost update = success − finalSoldQty", 69, 380, 520, C.ink, 20);
  bullet(s, "Utilization = finalSoldQty / 96", 69, 431, 520, C.ink, 20);
  bullet(s, "Trung bình ± độ lệch chuẩn, n = 3", 69, 482, 520, C.ink, 20);
  panel(s, 658, 190, 581, 383, C.ink);
  text(s, "Một cơ chế PASS khi", 690, 222, 517, 36, { size: 25, bold: true, color: C.white });
  text(s, "01", 690, 291, 54, 35, { size: 20, bold: true, color: C.teal });
  text(s, "Âm kho = 0%", 759, 286, 420, 40, { size: 24, bold: true, color: C.white });
  text(s, "02", 690, 372, 54, 35, { size: 20, bold: true, color: C.teal });
  text(s, "Vi phạm nhất quán = 0%", 759, 367, 420, 40, { size: 24, bold: true, color: C.white });
  text(s, "03", 690, 453, 54, 35, { size: 20, bold: true, color: C.teal });
  text(s, "TPS giảm ≤ 30% so với SYNC", 759, 448, 420, 46, { size: 24, bold: true, color: C.white });
}

// 9 — Raw results.
{
  const s = baseSlide("Evidence", "Dữ liệu thô của 12 lượt chạy", "OK/Sold/Lost là số lượng; Time tính bằng ms; TPS là transaction/second.");
  const rows = [
    ["Lần", "Cơ chế", "OK", "Sold", "Lost", "Time", "TPS", "Âm kho"],
    ["1", "NO_LOCK", "1000", "1", "999", "896.7", "1115.2", "90.4%"],
    ["1", "FILE_LOCK", "43", "43", "0", "381.9", "112.6", "0%"],
    ["1", "SYNCHRONIZED", "96", "96", "0", "1853.7", "51.8", "0%"],
    ["1", "OPTIMISTIC", "96", "96", "0", "2199.8", "43.6", "0%"],
    ["2", "NO_LOCK", "1000", "1", "999", "526.9", "1898.0", "90.4%"],
    ["2", "FILE_LOCK", "73", "73", "0", "360.6", "202.4", "0%"],
    ["2", "SYNCHRONIZED", "96", "96", "0", "1762.7", "54.5", "0%"],
    ["2", "OPTIMISTIC", "96", "96", "0", "2170.4", "44.2", "0%"],
    ["3", "NO_LOCK", "1000", "1", "999", "941.9", "1061.7", "90.4%"],
    ["3", "FILE_LOCK", "59", "59", "0", "352.6", "167.3", "0%"],
    ["3", "SYNCHRONIZED", "96", "96", "0", "2210.7", "43.4", "0%"],
    ["3", "OPTIMISTIC", "96", "96", "0", "3054.2", "31.4", "0%"],
  ];
  manualTable(s, 41, 186, [70, 224, 100, 100, 100, 140, 140, 150], rows, { rowH: 34, mechanismCol: 1, bodySize: 13 });
  text(s, "Quan sát ổn định nhất: NO_LOCK luôn mất 999 cập nhật; ba cơ chế còn lại không vi phạm toàn vẹn.", 41, 641, 1198, 26, { size: 15, bold: true, color: C.red, align: "center" });
}

// 10 — Aggregate cards.
{
  const s = baseSlide("Aggregate result", "Trung bình của ba lần lặp");
  mechanismCard(s, 41, 204, 278, "NO_LOCK", "1,358.3", "90.4", "1.0", "FAIL");
  mechanismCard(s, 343, 204, 278, "FILE_LOCK", "160.8", "0.0", "60.8", "PASS*");
  mechanismCard(s, 645, 204, 278, "SYNCHRONIZED", "49.9", "0.0", "100", "PASS");
  mechanismCard(s, 947, 204, 292, "OPTIMISTIC", "39.8", "0.0", "100", "PASS");
  text(s, "* FILE_LOCK đạt tiêu chí số học nhưng không được khuyến nghị cho nhiều thread trong cùng JVM.", 41, 581, 1198, 28, { size: 16, bold: true, color: C.amber, align: "center" });
}

// 11 — Primary chart.
{
  const s = baseSlide("Primary evidence", "Throughput vs. tỷ lệ âm kho", "Trục throughput dùng log scale để nhìn đủ cả bốn cơ chế.");
  await addImage(s, path.join(FIG, "throughput_vs_negative_stock_rate.png"), 41, 178, 845, 452, "Biểu đồ throughput và tỷ lệ âm kho");
  panel(s, 916, 197, 323, 385, C.panel);
  text(s, "Đọc biểu đồ", 944, 225, 267, 35, { size: 23, bold: true });
  bullet(s, "NO_LOCK nhanh nhưng nằm ở 90.4% âm kho.", 944, 286, 256, C.ink, 17);
  bullet(s, "Ba điểm an toàn đều nằm trên đường 0%.", 944, 367, 256, C.ink, 17);
  bullet(s, "OPTIMISTIC chỉ giảm 20.3% so với SYNC.", 944, 448, 256, C.ink, 17);
}

// 12 — Repeat variation.
{
  const s = baseSlide("Repeatability", "Độ biến thiên throughput qua ba lần lặp");
  await addImage(s, path.join(FIG, "throughput_by_repeat.png"), 41, 184, 810, 434, "Biểu đồ throughput theo lần lặp");
  panel(s, 884, 202, 355, 360, C.white, C.line);
  text(s, "Mean ± SD", 912, 230, 299, 32, { size: 22, bold: true });
  text(s, "NO_LOCK", 912, 294, 135, 24, { size: 15, bold: true, color: C.red });
  text(s, "1,358.3 ± 468.2", 1060, 294, 151, 24, { size: 16, bold: true, align: "right" });
  text(s, "FILE_LOCK", 912, 352, 135, 24, { size: 15, bold: true, color: C.teal });
  text(s, "160.8 ± 45.3", 1060, 352, 151, 24, { size: 16, bold: true, align: "right" });
  text(s, "SYNCHRONIZED", 912, 410, 155, 24, { size: 15, bold: true, color: C.blue });
  text(s, "49.9 ± 5.8", 1060, 410, 151, 24, { size: 16, bold: true, align: "right" });
  text(s, "OPTIMISTIC", 912, 468, 135, 24, { size: 15, bold: true, color: C.purple });
  text(s, "39.8 ± 7.2", 1060, 468, 151, 24, { size: 16, bold: true, align: "right" });
}

// 13 — Integrity/utilization.
{
  const s = baseSlide("Integrity", "Tốc độ cao không đồng nghĩa xử lý đúng");
  await addImage(s, path.join(FIG, "integrity_and_inventory_utilization.png"), 41, 180, 820, 440, "Biểu đồ vi phạm nhất quán và khai thác tồn kho");
  panel(s, 892, 205, 347, 352, C.redSoft);
  text(s, "Phát hiện quan trọng", 920, 233, 291, 34, { size: 22, bold: true, color: C.red });
  text(s, "99.9%", 920, 301, 291, 54, { size: 43, bold: true, color: C.red });
  text(s, "vi phạm nhất quán của NO_LOCK", 920, 359, 291, 42, { size: 17, color: C.ink });
  text(s, "1.0%", 920, 439, 291, 47, { size: 36, bold: true, color: C.red });
  text(s, "kho thực sự được ghi nhận", 920, 493, 291, 36, { size: 17, color: C.ink });
}

// 14 — Race timeline.
{
  const s = baseSlide("Root cause", "Lost update xảy ra như thế nào trong NO_LOCK?");
  const cols = [62, 337, 612, 887];
  const titles = ["T1 đọc", "T2 đọc", "T1 ghi", "T2 ghi đè"];
  const values = ["soldQty = 0", "soldQty = 0", "soldQty = 1", "soldQty = 1"];
  for (let i = 0; i < 4; i++) {
    shape(s, "ellipse", cols[i] + 82, 209, 56, 56, i < 2 ? C.blueSoft : C.redSoft, i < 2 ? C.blue : C.red, 2);
    text(s, String(i + 1), cols[i] + 82, 220, 56, 28, { size: 18, bold: true, color: i < 2 ? C.blue : C.red, align: "center" });
    panel(s, cols[i], 298, 220, 170, C.panel);
    text(s, titles[i], cols[i] + 18, 326, 184, 32, { size: 22, bold: true });
    text(s, values[i], cols[i] + 18, 382, 184, 30, { size: 19, color: i === 3 ? C.red : C.ink, bold: i === 3 });
    if (i < 3) addArrow(s, cols[i] + 220, 364, 55, i < 1 ? C.blue : C.red);
  }
  panel(s, 62, 514, 1045, 84, C.redSoft);
  text(s, "Hai đơn đều thành công, nhưng file chỉ tăng 1 → một cập nhật bị mất; lặp 1.000 lần tạo 999 lost updates.", 90, 536, 989, 42, { size: 21, bold: true, color: C.red, align: "center" });
}

// 15 — FILE_LOCK.
{
  const s = baseSlide("Mechanism review", "FILE_LOCK: PASS số học, nhưng TPS cao vì nhiều đơn fail-fast");
  statCard(s, 41, 205, 278, "160.8", "TPS trung bình", C.teal, C.tealSoft);
  statCard(s, 343, 205, 278, "0%", "Âm kho & violation", C.teal, C.tealSoft);
  statCard(s, 645, 205, 278, "58.3", "Đơn thành công trung bình", C.amber, C.amberSoft);
  statCard(s, 947, 205, 292, "60.8%", "Khai thác kho", C.amber, C.amberSoft);
  panel(s, 41, 385, 1198, 190, C.amberSoft);
  text(s, "Caveat triển khai", 72, 413, 270, 34, { size: 24, bold: true, color: C.amber });
  bullet(s, "Java FileLock không phù hợp để điều phối nhiều thread trong cùng JVM.", 72, 468, 1110, C.ink, 19);
  bullet(s, "OverlappingFileLockException làm nhiều yêu cầu thất bại thay vì chờ.", 72, 519, 1110, C.ink, 19);
}

// 16 — SYNCHRONIZED.
{
  const s = baseSlide("Recommendation", "SYNCHRONIZED: phương án cân bằng cho ứng dụng hiện tại");
  panel(s, 41, 188, 566, 390, C.ink);
  text(s, "49.9 TPS", 75, 229, 498, 64, { size: 48, bold: true, color: C.white });
  text(s, "0% âm kho · 0% violation · 100% khai thác kho", 75, 311, 498, 65, { size: 24, bold: true, color: C.teal });
  text(s, "Monitor nội bộ JVM tuần tự hóa đúng vùng read–check–write và phù hợp kiến trúc một tiến trình của project.", 75, 412, 498, 116, { size: 21, color: C.white });
  panel(s, 642, 188, 597, 390, C.blueSoft);
  text(s, "Vì sao chọn?", 676, 223, 529, 38, { size: 27, bold: true, color: C.blue });
  bullet(s, "Đơn giản, dễ giải thích và dễ kiểm thử.", 676, 296, 500, C.ink, 20);
  bullet(s, "Bán đủ 96/96 sản phẩm trong cả ba lần.", 676, 357, 500, C.ink, 20);
  bullet(s, "Không có retry storm dưới hotspot cực cao.", 676, 418, 500, C.ink, 20);
  bullet(s, "Là baseline an toàn hợp lý để so sánh.", 676, 479, 500, C.ink, 20);
}

// 17 — OPTIMISTIC.
{
  const s = baseSlide("Mechanism review", "OPTIMISTIC: an toàn, nhưng trả giá bằng retry");
  statCard(s, 41, 205, 278, "39.8", "TPS trung bình", C.purple, C.purpleSoft);
  statCard(s, 343, 205, 278, "−20.3%", "So với SYNCHRONIZED", C.purple, C.purpleSoft);
  statCard(s, 645, 205, 278, "0%", "Âm kho & violation", C.teal, C.tealSoft);
  statCard(s, 947, 205, 292, "96/96", "Kho được bán đúng", C.teal, C.tealSoft);
  panel(s, 41, 394, 1198, 184, C.purpleSoft);
  text(s, "Ý nghĩa", 73, 424, 180, 32, { size: 24, bold: true, color: C.purple });
  bullet(s, "Version check ngăn stale write và giữ đúng toàn vẹn.", 73, 475, 1090, C.ink, 19);
  bullet(s, "Khi 1.000 thread tranh cùng một record, retry làm total time tăng lên 2.475 ms.", 73, 526, 1090, C.ink, 19);
}

// 18 — Decision matrix.
{
  const s = baseSlide("Research question", "Ma trận quyết định theo đúng tiêu chí");
  const rows = [
    ["Cơ chế", "Âm kho = 0", "Violation = 0", "Δ TPS vs SYNC", "RQ", "Khuyến nghị"],
    ["NO_LOCK", "Không", "Không", "+2622.5%", "FAIL", "Đối chứng âm"],
    ["FILE_LOCK", "Có", "Có", "+222.2%", "PASS*", "Không chọn"],
    ["SYNCHRONIZED", "Có", "Có", "0.0%", "PASS", "CHỌN"],
    ["OPTIMISTIC", "Có", "Có", "−20.3%", "PASS", "Phương án 2"],
  ];
  manualTable(s, 41, 208, [225, 175, 190, 190, 150, 268], rows, { rowH: 65, mechanismCol: 0, bodySize: 16, headerSize: 13 });
  panel(s, 41, 564, 1198, 62, C.tealSoft);
  text(s, "Kết luận chính thức: FILE_LOCK, SYNCHRONIZED và OPTIMISTIC đạt tiêu chí hẹp; chọn SYNCHRONIZED để triển khai.", 65, 582, 1150, 28, { size: 18, bold: true, color: C.teal, align: "center" });
}

// 19 — Baseline sensitivity.
{
  const s = baseSlide("Interpretation", "Tại sao không dùng NO_LOCK làm baseline?", "Baseline phải là một phương án đúng về mặt chức năng.");
  panel(s, 41, 195, 565, 367, C.redSoft);
  text(s, "Nếu dùng NO_LOCK", 73, 226, 500, 38, { size: 27, bold: true, color: C.red });
  text(s, "−88.2%", 73, 298, 155, 45, { size: 35, bold: true, color: C.red });
  text(s, "FILE_LOCK", 247, 308, 280, 30, { size: 19, bold: true });
  text(s, "−96.3%", 73, 375, 155, 45, { size: 35, bold: true, color: C.red });
  text(s, "SYNCHRONIZED", 247, 385, 280, 30, { size: 19, bold: true });
  text(s, "−97.1%", 73, 452, 155, 45, { size: 35, bold: true, color: C.red });
  text(s, "OPTIMISTIC", 247, 462, 280, 30, { size: 19, bold: true });
  panel(s, 641, 195, 598, 367, C.panel);
  text(s, "Vấn đề logic", 675, 226, 530, 38, { size: 27, bold: true });
  bullet(s, "NO_LOCK đạt TPS cao nhờ bỏ qua tính đúng đắn.", 675, 299, 500, C.ink, 20);
  bullet(s, "So sánh với nó khiến mọi phương án an toàn đều “thua”.", 675, 372, 500, C.ink, 20);
  bullet(s, "SYNCHRONIZED là mốc an toàn, đầy đủ và tái lập.", 675, 445, 500, C.ink, 20);
}

// 20 — Closing answer.
{
  const s = presentation.slides.add();
  s.background.fill = C.ink;
  shape(s, "rect", 41, 36, 58, 6, C.teal);
  text(s, "FINAL ANSWER", 41, 58, 300, 24, { size: 13, bold: true, color: C.teal });
  text(s, "Lost update là race condition chính.", 41, 147, 1198, 68, { size: 43, bold: true, color: C.white });
  text(s, "Ba cơ chế đạt tiêu chí định lượng; SYNCHRONIZED là lựa chọn triển khai tốt nhất cho project CSV một JVM.", 41, 240, 1120, 104, { size: 31, color: C.white });
  panel(s, 41, 397, 370, 145, C.redSoft);
  text(s, "NO_LOCK", 67, 422, 318, 30, { size: 20, bold: true, color: C.red });
  text(s, "Đối chứng âm\n90.4% âm kho", 67, 467, 318, 58, { size: 20, bold: true, color: C.ink });
  panel(s, 455, 397, 370, 145, C.blueSoft);
  text(s, "SYNCHRONIZED", 481, 422, 318, 30, { size: 20, bold: true, color: C.blue });
  text(s, "Khuyến nghị\n0% lỗi · 100% kho", 481, 467, 318, 58, { size: 20, bold: true, color: C.ink });
  panel(s, 869, 397, 370, 145, C.purpleSoft);
  text(s, "OPTIMISTIC", 895, 422, 318, 30, { size: 20, bold: true, color: C.purple });
  text(s, "Phương án 2\nTPS chỉ giảm 20.3%", 895, 467, 318, 58, { size: 20, bold: true, color: C.ink });
  text(s, "Q&A · Dữ liệu thô, mã runner và công thức metric nằm trong thư mục reports/experiment", 41, 620, 1120, 32, { size: 16, color: C.line });
  text(s, "20", 1184, 670, 54, 22, { size: 12, color: C.line, align: "right" });
}

async function writeBlob(file, blob) {
  await fs.writeFile(file, new Uint8Array(await blob.arrayBuffer()));
}

async function main() {
  await fs.mkdir(OUT, { recursive: true });
  for (const [index, slide] of presentation.slides.items.entries()) {
    const stem = `slide-${String(index + 1).padStart(2, "0")}`;
    const png = await presentation.export({ slide, format: "png", scale: 1 });
    await writeBlob(path.join(OUT, `${stem}.png`), png);
    const layout = await slide.export({ format: "layout" });
    await fs.writeFile(path.join(OUT, `${stem}.layout.json`), await layout.text());
  }
  const montage = await presentation.export({ format: "webp", montage: true, scale: 0.5 });
  await writeBlob(path.join(OUT, "deck-montage.webp"), montage);
  const pptx = await PresentationFile.exportPptx(presentation);
  await pptx.save(PPTX);
  console.log(`Wrote ${presentation.slides.items.length} slides to ${PPTX}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
