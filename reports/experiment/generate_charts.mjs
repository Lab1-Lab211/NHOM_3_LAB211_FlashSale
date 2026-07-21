import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.dirname(fileURLToPath(import.meta.url));
const figures = path.join(root, "figures");
await fs.mkdir(figures, { recursive: true });

const summary = JSON.parse(await fs.readFile(path.join(root, "analysis_summary.json"), "utf8"));
const byName = Object.fromEntries(summary.summary.map((row) => [row.mechanism, row]));
const rawText = await fs.readFile(path.join(root, "raw_results_1000_threads_4_mechanisms_3_repeats.csv"), "utf8");

const colors = {
  NO_LOCK: "#D92D20",
  FILE_LOCK: "#0E9384",
  SYNCHRONIZED: "#2563EB",
  OPTIMISTIC: "#7A5AF8",
};
const labels = {
  NO_LOCK: "NO_LOCK",
  FILE_LOCK: "FILE_LOCK",
  SYNCHRONIZED: "SYNCHRONIZED",
  OPTIMISTIC: "OPTIMISTIC",
};

function escapeXml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&apos;",
  })[char]);
}

function text(x, y, value, size = 24, weight = 400, anchor = "start", fill = "#101828") {
  return `<text x="${x}" y="${y}" font-family="Arial, sans-serif" font-size="${size}" font-weight="${weight}" text-anchor="${anchor}" fill="${fill}">${escapeXml(value)}</text>`;
}

function line(x1, y1, x2, y2, stroke = "#D0D5DD", width = 2, dash = "") {
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${stroke}" stroke-width="${width}"${dash ? ` stroke-dasharray="${dash}"` : ""}/>`;
}

async function save(name, svg, width = 1600, height = 900) {
  const svgPath = path.join(figures, `${name}.svg`);
  await fs.writeFile(svgPath, svg, "utf8");
  return svgPath;
}

// Main research chart: logarithmic throughput axis keeps all four mechanisms readable.
{
  const W = 1600, H = 900;
  const plot = { x: 185, y: 165, w: 1290, h: 570 };
  const minLog = Math.log10(25), maxLog = Math.log10(2500);
  const x = (tps) => plot.x + (Math.log10(tps) - minLog) / (maxLog - minLog) * plot.w;
  const y = (rate) => plot.y + plot.h - rate / 100 * plot.h;
  const xTicks = [25, 50, 100, 200, 500, 1000, 2000];
  const yTicks = [0, 20, 40, 60, 80, 100];
  let body = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">`;
  body += `<rect width="${W}" height="${H}" fill="#FFFFFF"/>`;
  body += text(80, 70, "Throughput vs. tỷ lệ âm kho", 46, 700);
  body += text(80, 112, "Trung bình 3 lần lặp · 1.000 threads · trục TPS theo thang log", 24, 400, "start", "#475467");
  body += `<rect x="${x(34.9241)}" y="${y(2)}" width="${plot.x + plot.w - x(34.9241)}" height="${y(0) - y(2)}" fill="#D1FADF"/>`;
  for (const tick of yTicks) {
    const yy = y(tick);
    body += line(plot.x, yy, plot.x + plot.w, yy, tick === 0 ? "#667085" : "#EAECF0", tick === 0 ? 3 : 2);
    body += text(plot.x - 24, yy + 8, `${tick}%`, 22, 400, "end", "#475467");
  }
  for (const tick of xTicks) {
    const xx = x(tick);
    body += line(xx, plot.y, xx, plot.y + plot.h, "#F2F4F7", 2);
    body += text(xx, plot.y + plot.h + 42, tick.toLocaleString("vi-VN"), 21, 400, "middle", "#475467");
  }
  const threshold = x(34.9241);
  body += line(threshold, plot.y, threshold, plot.y + plot.h, "#12B76A", 3, "10 8");
  body += text(threshold + 10, plot.y + 28, "Ngưỡng 70% TPS của SYNCHRONIZED = 34,9", 19, 700, "start", "#027A48");
  body += text(38, plot.y + plot.h / 2, "Tỷ lệ âm kho logic", 25, 700, "middle", "#344054").replace(`<text x="38"`, `<text transform="rotate(-90 38 ${plot.y + plot.h / 2})" x="38"`);
  body += text(plot.x + plot.w / 2, 845, "Throughput trung bình (giao dịch thành công/giây)", 25, 700, "middle", "#344054");
  const offsets = {
    NO_LOCK: { dx: -26, dy: -28, anchor: "end" },
    FILE_LOCK: { dx: 25, dy: -96, anchor: "start" },
    SYNCHRONIZED: { dx: 30, dy: -82, anchor: "start" },
    OPTIMISTIC: { dx: -24, dy: -72, anchor: "end" },
  };
  for (const name of Object.keys(colors)) {
    const row = byName[name];
    const px = x(Number(row.meanThroughputTps));
    const py = y(Number(row.meanNegativeStockRatePct));
    const o = offsets[name];
    body += `<circle cx="${px}" cy="${py}" r="15" fill="${colors[name]}" stroke="#FFFFFF" stroke-width="5"/>`;
    body += line(px, py, px + o.dx * 0.75, py + o.dy * 0.75, colors[name], 2);
    body += text(px + o.dx, py + o.dy, labels[name], 23, 700, o.anchor, colors[name]);
    body += text(px + o.dx, py + o.dy + 28, `${Number(row.meanThroughputTps).toFixed(1)} TPS · ${Number(row.meanNegativeStockRatePct).toFixed(1)}%`, 20, 400, o.anchor, "#344054");
  }
  body += text(1465, 790, "Vùng đạt mục tiêu", 20, 700, "end", "#027A48");
  body += `</svg>`;
  await save("throughput_vs_negative_stock_rate", body, W, H);
}

// Throughput variability across the three measured repeats.
{
  const lines = rawText.replace(/^\uFEFF/, "").trim().split(/\r?\n/);
  const headers = lines[0].split(",");
  const rows = lines.slice(1).map((row) => {
    const values = row.split(",");
    return Object.fromEntries(headers.map((header, index) => [header, values[index]]));
  });
  const W = 1600, H = 900;
  const plot = { x: 170, y: 150, w: 1320, h: 590 };
  const yMin = Math.log10(25), yMax = Math.log10(2500);
  const y = (value) => plot.y + plot.h - (Math.log10(value) - yMin) / (yMax - yMin) * plot.h;
  const xs = [plot.x + 240, plot.x + 660, plot.x + 1080];
  const ticks = [25, 50, 100, 200, 500, 1000, 2000];
  let body = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">`;
  body += `<rect width="${W}" height="${H}" fill="#FFFFFF"/>`;
  body += text(80, 70, "Độ biến thiên throughput giữa 3 lần lặp", 46, 700);
  body += text(80, 112, "NO_LOCK nhanh nhưng biến thiên lớn; ba cơ chế an toàn ổn định hơn", 24, 400, "start", "#475467");
  for (const tick of ticks) {
    const yy = y(tick);
    body += line(plot.x, yy, plot.x + plot.w, yy, "#EAECF0", 2);
    body += text(plot.x - 22, yy + 8, tick.toLocaleString("vi-VN"), 21, 400, "end", "#475467");
  }
  for (let repeat = 1; repeat <= 3; repeat++) {
    body += text(xs[repeat - 1], 790, `Lần ${repeat}`, 24, 700, "middle", "#344054");
  }
  for (const name of Object.keys(colors)) {
    const mechanismRows = rows.filter((row) => row.mechanism === name).sort((a, b) => Number(a.repeat) - Number(b.repeat));
    const points = mechanismRows.map((row, index) => `${xs[index]},${y(Number(row.throughputTps))}`).join(" ");
    body += `<polyline points="${points}" fill="none" stroke="${colors[name]}" stroke-width="5"/>`;
    mechanismRows.forEach((row, index) => {
      body += `<circle cx="${xs[index]}" cy="${y(Number(row.throughputTps))}" r="10" fill="${colors[name]}" stroke="#FFFFFF" stroke-width="3"/>`;
    });
  }
  let lx = 320;
  for (const name of Object.keys(colors)) {
    body += `<circle cx="${lx}" cy="845" r="9" fill="${colors[name]}"/>`;
    body += text(lx + 20, 853, name, 20, 700, "start", "#344054");
    lx += name === "SYNCHRONIZED" ? 350 : 290;
  }
  body += text(38, plot.y + plot.h / 2, "TPS (thang log)", 25, 700, "middle", "#344054").replace(`<text x="38"`, `<text transform="rotate(-90 38 ${plot.y + plot.h / 2})" x="38"`);
  body += `</svg>`;
  await save("throughput_by_repeat", body, W, H);
}

// Integrity and inventory-use comparison makes the FILE_LOCK caveat visible.
{
  const W = 1600, H = 900;
  const names = Object.keys(colors);
  const panelW = 665, panelH = 555, top = 190;
  const leftX = 100, rightX = 835;
  let body = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">`;
  body += `<rect width="${W}" height="${H}" fill="#FFFFFF"/>`;
  body += text(80, 70, "An toàn dữ liệu và mức sử dụng tồn kho", 46, 700);
  body += text(80, 112, "FILE_LOCK giữ an toàn nhưng bỏ lại trung bình 39,2% tồn kho chưa bán", 24, 400, "start", "#475467");
  body += text(leftX, 165, "Vi phạm nhất quán", 28, 700);
  body += text(rightX, 165, "Mức sử dụng 96 đơn vị tồn kho", 28, 700);
  body += `<rect x="${leftX}" y="${top}" width="${panelW}" height="${panelH}" fill="#F9FAFB" stroke="#EAECF0"/>`;
  body += `<rect x="${rightX}" y="${top}" width="${panelW}" height="${panelH}" fill="#F9FAFB" stroke="#EAECF0"/>`;
  names.forEach((name, index) => {
    const row = byName[name];
    const yy = top + 65 + index * 120;
    const rate = Number(row.meanConsistencyViolationRatePct);
    const util = Number(row.inventoryUtilizationPct);
    body += text(leftX + 20, yy + 18, name, 21, 700);
    body += `<rect x="${leftX + 215}" y="${yy - 8}" width="${400 * rate / 100}" height="34" fill="${colors[name]}"/>`;
    body += text(rate > 80 ? leftX + 600 : leftX + 630, yy + 18, `${rate.toFixed(1)}%`, 22, 700, "end", rate > 80 ? "#FFFFFF" : colors[name]);
    body += text(rightX + 20, yy + 18, name, 21, 700);
    body += `<rect x="${rightX + 215}" y="${yy - 8}" width="${400 * util / 100}" height="34" fill="${colors[name]}"/>`;
    body += text(util > 80 ? rightX + 600 : rightX + 630, yy + 18, `${util.toFixed(1)}%`, 22, 700, "end", util > 80 ? "#FFFFFF" : colors[name]);
  });
  body += text(800, 825, "Lưu ý: throughput cao do fail-fast không đồng nghĩa xử lý đơn hàng tốt.", 24, 700, "middle", "#B42318");
  body += `</svg>`;
  await save("integrity_and_inventory_utilization", body, W, H);
}

console.log(figures);
