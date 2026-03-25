[Setup]
AppId={{9A2B4C5D-1234-5678-ABCD-SNOWWALLET01}
AppName=SnowWallet
AppVersion=1.0.0
AppPublisher=Zanini

; Mantendo suas especificações exatas:
DefaultDirName={userdocs}\SnowWallet
DisableProgramGroupPage=yes

; Local onde o Instalador .exe será salvo:
OutputDir=C:\Users\Pedro\Desktop
OutputBaseFilename=Instalador_SnowWallet

; Caminho absoluto para o ícone DO INSTALADOR (correto):
SetupIconFile=C:\Users\Pedro\Documents\Projetos_Flutter\SnowWalletKMP\composeApp\src\desktopMain\resources\icon.ico

Compression=lzma2/ultra64
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=lowest

[Languages]
Name: "portuguese"; MessagesFile: "compiler:Languages\Portuguese.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
; Cópia da pasta gerada pelo Gradle do Compose:
Source: "C:\Users\Pedro\Documents\Projetos_Flutter\SnowWalletKMP\composeApp\build\compose\binaries\main\app\SnowWallet\SnowWallet.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\Pedro\Documents\Projetos_Flutter\SnowWalletKMP\composeApp\build\compose\binaries\main\app\SnowWallet\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; CORREÇÃO IMPORTANTE AQUI:
; O IconFilename não pode ter "C:\Users\Pedro\..." porque esse atalho será criado no PC do cliente final.
; Como o EXE do Compose já compila com o ícone embutido, nós apenas apontamos IconFilename para o próprio .exe instalado.
Name: "{userprograms}\SnowWallet"; Filename: "{app}\SnowWallet.exe"; IconFilename: "{app}\SnowWallet.exe"
Name: "{userdesktop}\SnowWallet"; Filename: "{app}\SnowWallet.exe"; Tasks: desktopicon; IconFilename: "{app}\SnowWallet.exe"

[Run]
Filename: "{app}\SnowWallet.exe"; Description: "{cm:LaunchProgram,SnowWallet}"; Flags: nowait postinstall skipifsilent
