Declare Function FindWindow Lib "user32" Alias _
    "FindWindowA" (ByVal lpClassName As String, ByVal lpWindowName _
    As String) As Long
Declare Function SendMessage Lib "user32" Alias _
    "SendMessageA" (ByVal hwnd As Long, ByVal wMsg As Long, ByVal _
    wParam As Long, ByVal lParam As Long) As Long
Declare Function SetForegroundWindow Lib "user32" _
	(ByVal hwnd As Long) As Long
Declare Sub CopyMemoryToPtr Lib "kernel32.dll" Alias "RtlMoveMemory" _
    (ByVal lDest As Long, Source As Any, ByVal Length As Long)
Declare Sub CopyMemoryToPtrStr Lib "kernel32.dll" Alias "RtlMoveMemory" _
    (ByVal lDest As Long, ByVal Source As String, ByVal Length As Long)
Declare Function VirtualFree Lib "kernel32" (ByVal lpAddr As Long, _
    ByVal lSize As Long, ByVal dwFreeType As Long) As Long
Declare Function VirtualAlloc Lib "kernel32" _
    (ByVal lpAddr As Long, ByVal lSize As Long, ByVal _
    flAllocationType As Long, ByVal flProtect As Long) As Long

Private Const WM_COPYDATA = &H4A
Private Const MEM_COMMIT = &H1000
Private Const MEM_RELEASE = &H8000
Private Const PAGE_READWRITE = 4
    
Sub ZoteroAddBibliography()
	Call ZoteroCommand("addBibliography", False)
End Sub

Sub ZoteroAddCitation()
	Call ZoteroCommand("addCitation", True)
End Sub

Sub ZoteroEditCitation()
	Call ZoteroCommand("editCitation", True)
End Sub

Sub ZoteroEditBibliography()
	Call ZoteroCommand("editBibliography", True)
End Sub

Sub ZoteroRefresh()
	Call ZoteroCommand("refresh", False)
End Sub

Sub ZoteroRemoveCodes()
	Call ZoteroCommand("removeCodes", False)
End Sub

Sub ZoteroSetDocPrefs()
	Call ZoteroCommand("setDocPrefs", True)
End Sub

Sub ZoteroNotFound()
	MsgBox("OpenOffice could not communicate with Zotero. Please ensure Firefox is open and try again.", MB_ICONSTOP)
End Sub

Sub ZoteroCommand(cmd As String, bringToFront As Boolean)
	Dim EnvironTest As String
	EnvironTest = Environ("OS")
	If EnvironTest <> "" Then
		' On Windows
		Dim ThWnd As Long, buf As Long, buf2 As Long, cds As Long, tmp As Long, _
			tmp2 As Long, a$ As String, totalLen As Long, sfa
		
		' Find Firefox message window
		Dim appNames(4)
		appNames(1) = "Firefox"
		appNames(2) = "Zotero"
		appNames(3) = "Browser"
		appNames(4) = "Minefield"
		For i = 1 To 4
			ThWnd = FindWindow(appNames(i) & "MessageWindow" & Chr(0), Chr(0))
			If ThWnd <> 0 Then
				Exit For
			End If
		Next
		If ThWnd = 0 Then
			ZoteroNotFound()
			Exit Sub
		End If
		
		' Bring to front if desired
		If bringToFront Then Call SetForegroundWindow(ThWnd)
		
		' Pass command-line args
		a$ = "firefox.exe -silent -ZoteroIntegrationAgent OpenOffice -ZoteroIntegrationCommand " & cmd & Chr(0) & "C:\" & Chr(0)
		buf = VirtualAlloc(0, 255, MEM_COMMIT, PAGE_READWRITE)
		Call CopyMemoryToPtrStr(buf, a$, Len(a$))
		totalLen = Len(a$)
		cds = VirtualAlloc(0, 12, MEM_COMMIT, PAGE_READWRITE)
		
		' Copy into COPYDATASTRUCT
		tmp = 1
		Call CopyMemoryToPtr(cds, tmp, 4)
		tmp2 = totalLen + 1
		Call CopyMemoryToPtr(cds + 4, tmp2, 4)
		Call CopyMemoryToPtr(cds + 8, buf, 4)
		
		' Send message
		Call SendMessage(ThWnd, WM_COPYDATA, 0, cds)
		
		' Free allocated memory
		Call VirtualFree(buf, 0, MEM_RELEASE)
		Call VirtualFree(cds, 0, MEM_RELEASE)
	Else
		' On another platform
		sfa = CreateUnoService("com.sun.star.ucb.SimpleFileAccess")
		On Error GoTo NotFound
		
		' For Zotero > 2.0 on OS X
		path$ = "/Users/Shared/.zoteroIntegrationPipe_" & Environ("LOGNAME")
		On Error GoTo ClearPath
		sfa.getSize("file://" & path$)
		On Error GoTo 0
		
		' For Zotero <= 2.0 on *NIX or OS X
		If path$ = "" Then
			path$ = Environ("HOME") & "/.zoteroIntegrationPipe"
			On Error GoTo ClearPath
			sfa.getSize("file://" & path$)
			On Error GoTo 0
		End If
		
		If path$ = "" Then
			GoTo NotFound
		End If
		
		Shell "bash -c ""echo 'OpenOffice " & cmd & "' > '" & path$ & "'"""
		Exit Sub
		
		ClearPath:
			path$ = ""
			Resume Next
		NotFound:
			ZoteroNotFound()
	End If
End Sub