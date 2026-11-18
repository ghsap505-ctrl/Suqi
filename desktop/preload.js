const { contextBridge } = require("electron");

contextBridge.exposeInMainWorld("souqiDesktop", {
  isDesktop: true,
  platform: process.platform,
});
