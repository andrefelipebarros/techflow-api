//go:build !windows

package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
)

func hide(cmd *exec.Cmd) {}

func localDataDir() string { return "" }

func showError(title, msg string) { fmt.Fprintln(os.Stderr, title+": "+msg) }

func versionProbe(launcher string) string { return launcher }

// Fora do Windows o lançador só serve para testar a lógica.
func javaCandidates() []string {
	var out []string
	if home := os.Getenv("JAVA_HOME"); home != "" {
		out = append(out, filepath.Join(home, "bin", "java"))
	}
	if p, err := exec.LookPath("java"); err == nil {
		out = append(out, p)
	}
	return out
}
