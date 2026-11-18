const { app, BrowserWindow, Menu, shell } = require("electron");
const fs = require("node:fs");
const path = require("node:path");

const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
}

let mainWindow = null;

function isAuthPopupUrl(urlString) {
  try {
    const u = new URL(urlString);
    return (
      u.hostname === "accounts.google.com" ||
      u.hostname.endsWith(".firebaseapp.com") ||
      u.hostname.endsWith(".web.app")
    );
  } catch {
    return false;
  }
}

function isExternalActionUrl(urlString) {
  try {
    const u = new URL(urlString);
    return u.protocol === "https:" || u.protocol === "http:" || u.protocol === "whatsapp:";
  } catch {
    return false;
  }
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 960,
    minHeight: 640,
    backgroundColor: "#FAF7F2",
    title: "Souqi Desktop | سوقي للحاسوب",
    autoHideMenuBar: false,
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webSecurity: true,
    },
  });

  // Intercept external links (such as https://wa.me/...) to open in system browser / WhatsApp Desktop
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (isAuthPopupUrl(url)) {
      return {
        action: "allow",
        overrideBrowserWindowOptions: {
          width: 520,
          height: 680,
          autoHideMenuBar: true,
          webPreferences: {
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: true,
          },
        },
      };
    }
    if (isExternalActionUrl(url)) {
      shell.openExternal(url);
    }
    return { action: "deny" };
  });

  mainWindow.webContents.on("will-navigate", (event, url) => {
    const isLocalFile = url.startsWith("file://");
    const devUrl = process.env.SOUQI_DESKTOP_URL;
    const isLocalDev = devUrl && url.startsWith(devUrl);
    if (!isLocalFile && !isLocalDev && isExternalActionUrl(url)) {
      event.preventDefault();
      shell.openExternal(url);
    }
  });

  const devUrl = process.env.SOUQI_DESKTOP_URL;
  const distIndex = path.join(__dirname, "web-dist", "index.html");

  if (devUrl) {
    mainWindow.loadURL(devUrl);
  } else if (fs.existsSync(distIndex)) {
    mainWindow.loadFile(distIndex);
  } else {
    mainWindow.loadURL(
      "data:text/html;charset=utf-8," +
        encodeURIComponent(`
          <!doctype html>
          <html lang="ar" dir="rtl">
            <head><meta charset="utf-8"><title>Souqi Desktop</title></head>
            <body style="font-family:system-ui,sans-serif;background:#FAF7F2;color:#111827;padding:48px;line-height:1.8;">
              <h2>لم يتم تجهيز حزمة الويب بعد</h2>
              <p>يرجى تشغيل الأمر التالي داخل مجلد <code>desktop</code> لبناء الواجهة ونسخها تلقائياً:</p>
              <pre style="background:#fff;padding:16px;border-radius:10px;border:1px solid #e5e7eb;" dir="ltr">npm run prepare-web && npm start</pre>
            </body>
          </html>
        `)
    );
  }
}

function buildAppMenu() {
  const template = [
    {
      label: "Souqi",
      submenu: [
        { role: "reload", label: "Reload / إعادة تحميل" },
        { role: "forceReload", label: "Force Reload" },
        { type: "separator" },
        { role: "resetZoom", label: "Reset Zoom" },
        { role: "zoomIn", label: "Zoom In" },
        { role: "zoomOut", label: "Zoom Out" },
        { type: "separator" },
        { role: "quit", label: "Quit / خروج" },
      ],
    },
    {
      label: "Edit",
      submenu: [
        { role: "undo" },
        { role: "redo" },
        { type: "separator" },
        { role: "cut" },
        { role: "copy" },
        { role: "paste" },
        { role: "selectAll" },
      ],
    },
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

app.whenReady().then(() => {
  buildAppMenu();
  createMainWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createMainWindow();
  });
});

app.on("second-instance", () => {
  if (mainWindow) {
    if (mainWindow.isMinimized()) mainWindow.restore();
    mainWindow.focus();
  }
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
