// TechFlow.exe: lançador que carrega o TechFlow.jar dentro do próprio executável,
// copia o jar para a pasta do usuário, procura um Java 17 ou mais novo e abre o app
// sem janela de console. Não usa nenhuma biblioteca além da padrão do Go.
package main

import (
	"crypto/sha256"
	_ "embed"
	"encoding/hex"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strconv"
)

//go:embed TechFlow.jar
var jarBytes []byte

var versionRe = regexp.MustCompile(`version "(\d+)(?:\.(\d+))?`)

const minJava = 17

func main() {
	jar, err := extractJar()
	if err != nil {
		showError("TechFlow", "Não foi possível preparar o aplicativo:\n"+err.Error())
		os.Exit(1)
	}

	java, found := findJava()
	if java == "" {
		msg := "O TechFlow precisa do Java 17 ou mais novo, e ele não foi encontrado neste computador.\n\n" +
			"Instale o JDK ou o JRE 21 (por exemplo, o Temurin em adoptium.net) e abra o TechFlow de novo."
		if found != "" {
			msg = "O TechFlow precisa do Java 17 ou mais novo, mas só foi encontrado: " + found + ".\n\n" +
				"Instale o JDK ou o JRE 21 (por exemplo, o Temurin em adoptium.net) e abra o TechFlow de novo."
		}
		showError("TechFlow", msg)
		os.Exit(1)
	}

	args := append([]string{"-Dfile.encoding=UTF-8", "-jar", jar}, os.Args[1:]...)
	cmd := exec.Command(java, args...)
	hide(cmd)
	if err := cmd.Start(); err != nil {
		showError("TechFlow", "Não foi possível abrir o Java:\n"+err.Error())
		os.Exit(1)
	}
}

// extractJar grava o jar embutido numa pasta do usuário, uma vez por versão.
func extractJar() (string, error) {
	sum := sha256.Sum256(jarBytes)
	name := "TechFlow-" + hex.EncodeToString(sum[:])[:12] + ".jar"
	dir := filepath.Join(cacheDir(), "TechFlow")
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return "", err
	}
	path := filepath.Join(dir, name)
	if st, err := os.Stat(path); err == nil && st.Size() == int64(len(jarBytes)) {
		return path, nil
	}
	tmp := path + ".tmp"
	if err := os.WriteFile(tmp, jarBytes, 0o644); err != nil {
		return "", err
	}
	if err := os.Rename(tmp, path); err != nil {
		return "", err
	}
	return path, nil
}

func cacheDir() string {
	if d := localDataDir(); d != "" {
		return d
	}
	if d, err := os.UserCacheDir(); err == nil {
		return d
	}
	return os.TempDir()
}

// findJava devolve o primeiro Java 17+ encontrado. Se só achar versões antigas,
// devolve o texto da versão encontrada para a mensagem de erro.
func findJava() (path string, tooOld string) {
	for _, c := range javaCandidates() {
		major := javaMajor(c)
		if major >= minJava {
			return c, ""
		}
		if major > 0 && tooOld == "" {
			tooOld = "Java " + strconv.Itoa(major)
		}
	}
	return "", tooOld
}

// javaMajor roda "java -version" e devolve a versão principal, ou 0 se falhar.
func javaMajor(launcher string) int {
	probe := versionProbe(launcher)
	cmd := exec.Command(probe, "-version")
	hide(cmd)
	out, err := cmd.CombinedOutput()
	if err != nil {
		return 0
	}
	m := versionRe.FindStringSubmatch(string(out))
	if m == nil {
		return 0
	}
	major, _ := strconv.Atoi(m[1])
	if major == 1 && m[2] != "" { // 1.8 significa Java 8
		major, _ = strconv.Atoi(m[2])
	}
	return major
}

func debugf(format string, a ...any) {
	if os.Getenv("TECHFLOW_DEBUG") != "" {
		fmt.Fprintf(os.Stderr, format+"\n", a...)
	}
}
