/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { FileSystemAPI } from "./FileSystemAPI";
import * as DailyRotateFile from 'winston-daily-rotate-file';

const winston = require('winston');

export const logger = winston.createLogger({
  format: winston.format.combine(
    winston.format.timestamp({
      format: 'YYYY-MM-DD HH:mm:ss.SSS'
    }),
    winston.format.prettyPrint()
  ),
  transports: []
});

export function setlogFile(fileName: string) {
  logger.add(new DailyRotateFile({
    filename: FileSystemAPI.resolvePath([logsDir(), fileName]),
    datePattern: 'YYYY-MM-DD',
    maxSize: '1m',
    maxFiles: '3d'
  }));
}

function logsDir(): string {
  const logsDir = FileSystemAPI.resolvePath([FileSystemAPI.tempDir(), "vscode-helidon", "plugin", "logs"]);
  if (!FileSystemAPI.isPathExistsSync(logsDir)) {
    FileSystemAPI.mkDir(logsDir);
  }
  return logsDir;
}

