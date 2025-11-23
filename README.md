<!-- ============================ -->
<!-- 📝 OTDO – Java Swing To-Do App -->
<!-- ============================ -->

<div align="center">

# 📝 **OTDO – Java Swing To-Do Application**
A modern, elegant, fully responsive **Java Swing desktop To-Do App** with  
categories, statistics chart, task filtering, and auto-saving.

---

### 🚀 Built With  
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/Swing-3A75C4?style=for-the-badge)
![OOP](https://img.shields.io/badge/OOP-SOLID-blue?style=for-the-badge)

</div>

---

## ⭐ **Features**
- ✔ Add / Edit / Delete Tasks  
- ✔ Mark as Completed  
- ✔ Categories (Add / Edit / Delete)  
- ✔ Search Tasks  
- ✔ Filter Active / Completed  
- ✔ Sort by Due Date  
- ✔ Task Priority (High, Medium, Low)  
- ✔ Pie Chart for task statistics  
- ✔ Sidebar navigation  
- ✔ Auto-save to local storage  
- ✔ Custom modern UI components  

---

## 🎨 **Screenshots**
assets/
├── banner.png
├── screenshot_home.png
├── screenshot_categories.png
├── screenshot_chart.png

yaml
Copy code

> (Add screenshots inside the `assets/` folder)

---

## ⚙ **Installation**
### **1️⃣ Compile**
```bash
javac *.java
2️⃣ Run
bash
Copy code
java Main
3️⃣ Create JAR
bash
Copy code
jar cfe OTDO.jar Main *.class
🏗 Project Structure
css
Copy code
src/
 ├── Main.java
 ├── Task.java
 ├── Category.java
 ├── StorageManager.java
 ├── UI/
 │   ├── Sidebar.java
 │   ├── TaskListPanel.java
 │   ├── HeaderBar.java
 │   ├── CategoryPanel.java
 │   ├── ChartPanel.java
 │   └── RoundedPanel.java
 └── util/
     ├── Colors.java
     ├── Utils.java
🔮 Future Improvements
Dark mode

Notifications

Cloud sync

Export tasks to CSV/JSON

🤝 Contributing
Pull requests are welcome! ❤️
