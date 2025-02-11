<!-- Plugin description -->

# KDocGen: Code & Documentation Generator

## 🛠️ Overview

KDocGen is a powerful IntelliJ IDEA plugin designed to streamline **code generation** and **documentation creation** for
Kotlin developers.  
With a single shortcut, you can instantly generate structured code, comprehensive documentation, and well-organized test
cases—reducing manual effort and boosting productivity.

Whether you're writing new functions, documenting existing ones, or setting up test cases, KDocGen ensures your codebase
remains **clean, well-documented, and maintainable**.  
By leveraging AI-assisted automation, this plugin helps you focus on writing high-quality logic while it takes care of
the repetitive tasks.

### Why use KDocGen?

✅ **Automate documentation** – No more writing KDocs manually.  
✅ **Generate missing code** – Quickly create function bodies and method stubs.  
✅ **Simplify testing** – Instantly generate Kotest test files for your functions.  
✅ **Enhance readability** – Insert meaningful comments with ease.  
✅ **Seamless IntelliJ IDEA integration** – Works smoothly with a simple shortcut.

Stay productive, write better code, and let KDocGen handle the rest! 🚀

---

## ✨ Features
- 🔹 **Generate function documentation**: Insert AI-assisted documentation for a selected function.
- 🔹 **Generate implementation (`IMPL`)**: Automatically create method stubs and function bodies.
- 🔹 **Generate comments**: Add meaningful comments for selected code.
- 🔹 **Seamless integration**: Works effortlessly within IntelliJ IDEA using a single shortcut.

---

## 🛠️ Installation
1. Open **IntelliJ IDEA**.
2. Go to **Plugins** → **Marketplace**.
3. Search for **KDocGen**.
4. Click **Install**.
5. Go to **Settings** → **KDocGen Settings** and enter your OpenAI API key.
6. Click **Save**, and you're ready to go!

---

## 🚀 Usage

#### ⚠️ Before you start, make sure that you have an `OpenAI API Token` ⚠️

### **Generating documentation for a function**

1. **Navigate to the function**.
2. **Press** [`⌘ + N`] (on macOS).
3. **Select** `✨ Generate function docs`.
4. The plugin will **generate KDoc** for the selected function automatically.

### **Generating any type of code**
1. **Write a comment** starting with `// IMPL:`.
2. Describe what you want to generate, for example:
   - `// IMPL π の値を小数点以下 10 桁まで計算する関数を記述します。`
   - `// IMPL Проверить, что строка является UUID`
3. **Feel free** to add as many `// IMPL` fragments as needed.
4. **Press** [`⌘ + N`] (on macOS).
5. **Select** `✨ Generate missing code`.
6. The plugin will **generate and insert the corresponding code block**.

### **Generating a Kotest test for a selected function**

1. **Navigate to the function**.
2. **Press** [`⌘ + N`] (on macOS).
3. **Select** `✨ Generate Kotest file`.
4. The plugin will generate a **scratch file** with a test for this function using `BehaviorSpec`.

### **Generating a comment for selected code (or text)**

1. **Select a piece of code** (e.g., a function or class).
2. **Press** [`⌘ + N`] (on macOS).
3. **Select** `✨ Describe code fragment`.
4. The plugin will **generate a comment** automatically.

---
## ⚡ Shortcut
| Action | Shortcut (macOS) |  
|--------|------------------|  
| Generate Code & Docs | `⌘ + N` |  

## 📌 Future Enhancements
- 🔹 Improved AI-based documentation generation.
- 🔹 Support for additional languages (e.g., Java).

---

💡 **Enhance your development workflow with automated code & documentation generation!**

---

### If you want to support the author or have an idea, visit [Boosty](https://boosty.to/sapotero/donate).

<!-- Plugin description end -->
