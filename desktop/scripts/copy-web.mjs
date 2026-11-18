import { execSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const desktopDir = __dirname;
const webDir = path.resolve(desktopDir, "..", "web");
const webDist = path.join(webDir, "dist");
const targetDist = path.join(desktopDir, "web-dist");

if (!fs.existsSync(webDir)) {
  console.error("Error: ../web directory not found.");
  process.exit(1);
}

console.log("Building Souqi Web bundle...");
execSync("npm install && npm run build", { cwd: webDir, stdio: "inherit" });

if (!fs.existsSync(webDist)) {
  console.error("Error: Web build output (../web/dist) not found.");
  process.exit(1);
}

fs.rmSync(targetDist, { recursive: true, force: true });
fs.cpSync(webDist, targetDist, { recursive: true });
console.log(`Copied web bundle to ${targetDist}`);
