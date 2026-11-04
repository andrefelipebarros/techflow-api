//go:build windows

package main

import (
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strings"
	"syscall"
	"unsafe"
)

const createNoWindow = 0x08000000

func hide(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true, CreationFlags: createNoWindow}
}

func localDataDir() string { return os.Getenv("LOCALAPPDATA") }

func showError(title, msg string) {
	user32 := syscall.NewLazyDLL("user32.dll")
	box := user32.NewProc("MessageBoxW")
	t, _ := syscall.UTF16PtrFromString(title)
	m, _ := syscall.UTF16PtrFromString(msg)
	const mbOkIconError = 0x10
	box.Call(0, uintptr(unsafe.Pointer(m)), uintptr(unsafe.Pointer(t)), mbOkIconError)
}

// versionProbe troca javaw.exe por java.exe, que é o que imprime a versão.
func versionProbe(launcher string) string {
	dir, file := filepath.Split(launcher)
	if strings.EqualFold(file, "javaw.exe") {
		if _, err := os.Stat(filepath.Join(dir, "java.exe")); err == nil {
			return filepath.Join(dir, "java.exe")
		}
	}
	return launcher
}

// javaCandidates lista os javaw.exe possíveis: JAVA_HOME, o PATH e as pastas
// onde os instaladores mais comuns colocam o Java, das versões mais novas para as antigas.
func javaCandidates() []string {
	var out []string
	seen := map[string]bool{}
	add := func(p string) {
		if p == "" || seen[strings.ToLower(p)] {
			return
		}
		if _, err := os.Stat(p); err == nil {
			seen[strings.ToLower(p)] = true
			out = append(out, p)
		}
	}
	if home := os.Getenv("JAVA_HOME"); home != "" {
		add(filepath.Join(home, "bin", "javaw.exe"))
	}
	if p, err := exec.LookPath("javaw.exe"); err == nil {
		add(p)
	}
	roots := []string{os.Getenv("ProgramFiles"), os.Getenv("ProgramFiles(x86)"), os.Getenv("LOCALAPPDATA")}
	vendors := []string{"Java", "Eclipse Adoptium", "Microsoft", "Amazon Corretto", "Zulu", "BellSoft", "Semeru", "Programs\\Eclipse Adoptium"}
	for _, root := range roots {
		if root == "" {
			continue
		}
		for _, v := range vendors {
			matches, _ := filepath.Glob(filepath.Join(root, v, "*", "bin", "javaw.exe"))
			sort.Sort(sort.Reverse(sort.StringSlice(matches)))
			for _, m := range matches {
				add(m)
			}
		}
	}
	return out
}
