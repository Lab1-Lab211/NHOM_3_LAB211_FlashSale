import fs from "node:fs/promises";
import path from "node:path";
import { Presentation } from "@oai/artifact-tool";

const projectRoot = path.resolve("../..");
const figures = path.join(projectRoot, "reports", "experiment", "figures");
const names = [
  "throughput_vs_negative_stock_rate",
  "throughput_by_repeat",
  "integrity_and_inventory_utilization",
];

async function writeBlob(filePath, blob) {
  await fs.writeFile(filePath, new Uint8Array(await blob.arrayBuffer()));
}

for (const name of names) {
  const svg = await fs.readFile(path.join(figures, `${name}.svg`));
  const presentation = Presentation.create({ slideSize: { width: 1600, height: 900 } });
  const slide = presentation.slides.add();
  slide.background.fill = "#FFFFFF";
  slide.images.add({
    blob: svg.buffer.slice(svg.byteOffset, svg.byteOffset + svg.byteLength),
    contentType: "image/svg+xml",
    alt: name,
    fit: "contain",
    position: { left: 0, top: 0, width: 1600, height: 900 },
  });
  await writeBlob(path.join(figures, `${name}.png`),
    await presentation.export({ slide, format: "png", scale: 1 }));
}

console.log(figures);
