declare const require: (id: string) => any;
declare const process: {
  argv: string[];
  cwd(): string;
  env: Record<string, string | undefined>;
  exit(code?: number): never;
};

const { spawnSync } = require("child_process") as {
  spawnSync: (
    command: string,
    args: string[],
    options: {
      stdio: "inherit";
      shell: true;
      env?: Record<string, string | undefined>;
    },
  ) => { status: number | null };
};
const { existsSync, readFileSync } = require("fs") as {
  existsSync: (path: string) => boolean;
  readFileSync: (path: string, encoding: "utf8") => string;
};

/** 與 Spring 設定對應的資料庫 profile */
type DbProfile = "mysql" | "h2";

/** `reset-db` 腳本的 CLI／執行選項 */
interface ResetOptions {
  /** 目標資料庫名稱（主要用於 MySQL） */
  dbName: string;
}

/** 腳本預設常數（可被環境變數或 CLI 覆寫） */
const DEFAULTS = {
  dbName: "mogumogu",
  mysqlHost: "localhost",
  mysqlPort: "3306",
  mysqlUser: "root",
};

/** 依 `SPRING_PROFILES_ACTIVE` 判斷使用 mysql 或 h2 profile */
function dbProfileFromSpringEnv(): DbProfile {
  const raw = process.env.SPRING_PROFILES_ACTIVE?.trim() ?? "";
  if (!raw) return "h2";
  const profiles = raw
    .split(",")
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean);
  return profiles.includes("mysql") ? "mysql" : "h2";
}

/** 讀取 `.env` 並合併至 `process.env`（不覆寫既有變數） */
function loadEnvFromFileIfExists(path: string): void {
  if (!existsSync(path)) return;

  const lines = readFileSync(path, "utf8").split(/\r?\n/);
  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#") || !line.includes("=")) continue;

    const separatorIndex = line.indexOf("=");
    const key = line.slice(0, separatorIndex).trim();
    const value = line.slice(separatorIndex + 1).trim();
    if (!process.env[key]) {
      process.env[key] = value;
    }
  }
}

/** 解析 CLI 參數（例如 `--dbName`） */
function parseArgs(argv: string[]): ResetOptions {
  const options: ResetOptions = {
    dbName: process.env.DB_NAME || DEFAULTS.dbName,
  };

  for (let i = 0; i < argv.length; i++) {
    const flag = argv[i];
    const value = argv[i + 1];

    if (flag === "--dbName" && value) {
      options.dbName = value;
      i++;
    }
  }

  return options;
}

/** 同步執行子程序，若結束碼非 0 則拋錯 */
function runOrThrow(command: string, args: string[]): void {
  const result = spawnSync(command, args, {
    stdio: "inherit",
    shell: true,
  });
  if (result.status !== 0) {
    throw new Error(`執行失敗: ${command} ${args.join(" ")}`);
  }
}

/** 產生用於刪除並重建 MySQL 資料庫的 SQL */
function buildMysqlResetSql(dbName: string): string {
  return `DROP DATABASE IF EXISTS ${dbName}; CREATE DATABASE ${dbName} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`;
}

/** 重置 MySQL（DROP／CREATE DB）後以 `spring-boot:run` 啟動（mysql profile） */
function resetMysql(options: ResetOptions): void {
  const mysqlUser = process.env.DB_USERNAME || DEFAULTS.mysqlUser;
  const mysqlPassword = process.env.DB_PASSWORD || "";
  const mysqlHost = DEFAULTS.mysqlHost;
  const mysqlPort = DEFAULTS.mysqlPort;
  const sql = buildMysqlResetSql(options.dbName);

  console.log(
    `Resetting MySQL database '${options.dbName}' on ${mysqlHost}:${mysqlPort} ...`,
  );

  const mysqlArgs = ["-h", mysqlHost, "-P", mysqlPort, "-u", mysqlUser];
  if (!mysqlPassword) {
    mysqlArgs.push("-p");
  }
  mysqlArgs.push("-e", `"${sql}"`);

  const result = spawnSync("mysql", mysqlArgs, {
    stdio: "inherit",
    shell: true,
    env: {
      ...process.env,
      MYSQL_PWD: mysqlPassword || process.env.MYSQL_PWD,
    },
  });
  if (result.status !== 0) {
    throw new Error(`執行失敗: mysql ${mysqlArgs.join(" ")}`);
  }

  runOrThrow("mvnw.cmd", [
    "spring-boot:run",
    '"-Dspring-boot.run.profiles=mysql"',
  ]);
}

/** 刪除 H2 檔案型資料庫後以 `spring-boot:run` 啟動（h2 profile） */
function resetH2(): void {
  console.log("Resetting H2 file database (./data/mogumogu*) ...");
  runOrThrow("powershell", [
    "-NoProfile",
    "-Command",
    "$ErrorActionPreference='SilentlyContinue'; Remove-Item -Path '.\\data\\mogumogu*' -Force; exit 0",
  ]);
  runOrThrow("mvnw.cmd", [
    "spring-boot:run",
    '"-Dspring-boot.run.profiles=h2"',
  ]);
}

/** 依環境 profile 執行 mysql 或 h2 重置流程的進入點 */
function main(): void {
  loadEnvFromFileIfExists(".env");

  const profile = dbProfileFromSpringEnv();
  const active =
    process.env.SPRING_PROFILES_ACTIVE?.trim() || "（未設定，等同 h2）";
  console.log(`[reset-db] SPRING_PROFILES_ACTIVE=${active} → ${profile}`);

  const options = parseArgs(process.argv.slice(2));
  if (profile === "mysql") {
    resetMysql(options);
    return;
  }

  resetH2();
}

main();
