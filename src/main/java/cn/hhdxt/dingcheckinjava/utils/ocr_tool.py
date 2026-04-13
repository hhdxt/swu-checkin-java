import sys
import os
import ddddocr
# 忽略一些无关紧要的警告（如果是旧版 Python 可能会有）
import warnings
warnings.filterwarnings('ignore')

def main():
    # 1. 校验是否接收到了 Java 传过来的图片路径
    if len(sys.argv) < 2:
        print("Error: Missing image path")
        sys.exit(1)

    img_path = sys.argv[1]

    # 2. 校验文件是否存在
    if not os.path.exists(img_path):
        print(f"Error: File not found {img_path}")
        sys.exit(1)

    try:
        # 3. 初始化 OCR 引擎
        # 【关键】show_ad=False 必须加上，否则会打印欢迎信息，污染标准输出
        ocr = ddddocr.DdddOcr(show_ad=False)

        # 4. 以二进制方式读取图片并进行识别
        with open(img_path, 'rb') as f:
            img_bytes = f.read()

        result = ocr.classification(img_bytes)

        # 5. 打印纯净的结果，供 Java 抓取
        print(result)

    except Exception as e:
        # 如果出错，打印带有 Error 前缀的信息，方便 Java 端判断
        print(f"Error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()