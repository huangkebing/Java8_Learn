1. java.util.jar是提供读写 JAR (Java ARchive) 文件格式的类，该格式基于具有可选清单文件的标准 ZIP 文件格式。
2. 清单存储与 JAR 文件内容有关的元信息，也用于签名 JAR 文件
3. java.util.zip 包基于以下规范：
   1. Info-ZIP 文件格式 - 基于 Info-ZIP 文件格式的 JAR 格式。参见 java.util.zip 包规范。
   2. 在 JAR 文件中，所有文件名必须以 UTF-8 编码进行编码。