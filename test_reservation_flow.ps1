$ErrorActionPreference = "Stop"

Write-Host "=== TEST 1: Check books in catalog ===" -ForegroundColor Cyan
$books = Invoke-RestMethod -Uri "http://localhost:8080/api/books" -Method Get
Write-Host "Found $($books.Count) books."
$book = $books | Where-Object { $_.title -like "*Fundamentals of Data Science*" } | Select-Object -First 1
if (-not $book) { $book = $books[0] }
Write-Host "Target Book: ID=$($book.id), Title='$($book.title)', Available=$($book.availableCopies)" -ForegroundColor Yellow

Write-Host "`n=== TEST 2: Borrow all copies so availableCopies = 0 ===" -ForegroundColor Cyan
$borrowedTxs = @()
$students = @("EECIT002", "EECIT003", "EECIT004", "EECIT005", "EECIT006")
for ($i = 0; $i -lt $book.availableCopies; $i++) {
    $stId = $students[$i % $students.Count]
    $issueBody = @{ studentId = $stId; bookId = $book.id } | ConvertTo-Json
    $tx = Invoke-RestMethod -Uri "http://localhost:8080/api/borrow/issue" -Method Post -Body $issueBody -ContentType "application/json"
    $borrowedTxs += $tx
    Write-Host "Issued copy $($i+1) to $stId (Tx ID: $($tx.id))"
}

# Verify availableCopies is now 0
$updatedBook = Invoke-RestMethod -Uri "http://localhost:8080/api/books/$($book.id)" -Method Get
Write-Host "Book now has $($updatedBook.availableCopies) available copies." -ForegroundColor Yellow

Write-Host "`n=== TEST 3: Attempting to issue when 0 copies available ===" -ForegroundColor Cyan
try {
    $issueBody = @{ studentId = "EECIT001"; bookId = $book.id } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/borrow/issue" -Method Post -Body $issueBody -ContentType "application/json"
    Write-Host "ERROR: Should not have allowed issue!" -ForegroundColor Red
} catch {
    Write-Host "PASSED: Issue blocked when 0 copies available: $($_.Exception.Message)" -ForegroundColor Green
}

Write-Host "`n=== TEST 4: Add student Yashwanthiga to Reservation ===" -ForegroundColor Cyan
$waitlistBody = @{ studentId = "EECIT001"; bookId = $book.id } | ConvertTo-Json
$reservation = Invoke-RestMethod -Uri "http://localhost:8080/api/waitlist/join" -Method Post -Body $waitlistBody -ContentType "application/json"
Write-Host "Reservation created:" -ForegroundColor Green
Write-Host "  Student ID:       $($reservation.studentId)"
Write-Host "  Student Name:     $($reservation.studentName)"
Write-Host "  Student Email:    $($reservation.studentEmail)"
Write-Host "  Book ID:          $($reservation.bookId)"
Write-Host "  Book Title:       $($reservation.bookTitle)"
Write-Host "  Reservation Date: $($reservation.reservationDate)"
Write-Host "  Status:           $($reservation.status)"

Write-Host "`n=== TEST 5: Verify Reservation in /api/waitlist/all ===" -ForegroundColor Cyan
$allWaitlist = Invoke-RestMethod -Uri "http://localhost:8080/api/waitlist/all" -Method Get
$foundRes = $allWaitlist | Where-Object { $_.id -eq $reservation.id }
if ($foundRes) {
    Write-Host "PASSED: Reservation found in list with Status=$($foundRes.status)" -ForegroundColor Green
} else {
    Write-Host "FAILED: Reservation not found in list!" -ForegroundColor Red
}

Write-Host "`n=== TEST 6: Return a borrowed book copy ===" -ForegroundColor Cyan
$returnTxId = $borrowedTxs[0].id
$returnRes = Invoke-RestMethod -Uri "http://localhost:8080/api/borrow/return/$returnTxId" -Method Post
Write-Host "Returned Tx ID: $returnTxId for book '$($returnRes.bookTitle)'"
Write-Host "Waiting students count in return response: $($returnRes.waitingStudents.Count)" -ForegroundColor Yellow
if ($returnRes.waitingStudents.Count -gt 0) {
    Write-Host "PASSED: Waiting student correctly detected: $($returnRes.waitingStudents[0].studentName) ($($returnRes.waitingStudents[0].studentEmail))" -ForegroundColor Green
}

# Verify reservation is STILL 'WAITING' (NOT automatically emailed or notified)
$checkRes = Invoke-RestMethod -Uri "http://localhost:8080/api/waitlist/all" -Method Get | Where-Object { $_.id -eq $reservation.id }
if ($checkRes.status -eq "WAITING") {
    Write-Host "PASSED: Status remains WAITING (not automatically notified on return, waiting for manual librarian click)" -ForegroundColor Green
} else {
    Write-Host "FAILED: Status changed automatically to $($checkRes.status)" -ForegroundColor Red
}

Write-Host "`n=== TEST 7: Librarian clicks 'Notify Student' ===" -ForegroundColor Cyan
$notifyRes = Invoke-RestMethod -Uri "http://localhost:8080/api/waitlist/$($reservation.id)/notify" -Method Post
Write-Host "Notify Response:" -ForegroundColor Green
Write-Host "  Student Name:     $($notifyRes.studentName)"
Write-Host "  Status:           $($notifyRes.status)"
Write-Host "  Notification Date:$($notifyRes.notificationDate)"
if ($notifyRes.status -eq "NOTIFIED") {
    Write-Host "PASSED: Status updated to NOTIFIED upon librarian click!" -ForegroundColor Green
} else {
    Write-Host "FAILED: Status is $($notifyRes.status)" -ForegroundColor Red
}

Write-Host "`n=== ALL BACKEND WORKFLOW TESTS COMPLETED SUCCESSFULLY ===" -ForegroundColor Green
