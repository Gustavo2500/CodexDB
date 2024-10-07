
<p align="center">
  <img src="/app/src/main/res/drawable/codexdb_banner.png" alt="drawing" width="850"/>
</p>
<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white">
  <img alt="Android" src="https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white">
  <img alt="SQLite" src="https://img.shields.io/badge/SQLite-07405E?style=flat&logo=sqlite&logoColor=white">
  <img alt="OpenLibrary API" src="https://img.shields.io/badge/OpenLibrary-API-blue">
  <img alt="ZXing library" src="https://img.shields.io/badge/ZXing-Library-white">
</p>
<p align="center">A simple book barcode scanner app that reads their ISBN and adds the information to a database.</p>

<h2>How it works.</h2>
<p style="text: justify">
Uses the device's camera to scan with ZXing the barcode of a physical book and reads its ISBN number. A request is sent to the OpenLibrary API to receive the book's       information and store it in a local database.
</p>
<p style="text: justify">
The app allows exporting the database contents to a PDF file.
</p>
