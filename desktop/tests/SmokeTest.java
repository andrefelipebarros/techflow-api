import com.alipe.techflow.desktop.model.Models.Priority;
import com.alipe.techflow.desktop.model.Models.Project;
import com.alipe.techflow.desktop.model.Models.ProjectStatus;
import com.alipe.techflow.desktop.model.Models.Task;
import com.alipe.techflow.desktop.model.Models.TaskStatus;
import com.alipe.techflow.desktop.net.ApiException;
import com.alipe.techflow.desktop.net.DemoTransport;
import com.alipe.techflow.desktop.net.Json;
import com.alipe.techflow.desktop.net.RequestLog;
import com.alipe.techflow.desktop.net.TechFlowApi;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Testes de fumaça da camada de rede e do servidor de demonstração, sem dependências. */
public class SmokeTest {
    private static int checks;

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) throw new AssertionError("FALHOU: " + what);
        System.out.println("ok  " + what);
    }

    private static int statusOf(Runnable r) {
        try { r.run(); return 200; } catch (ApiException e) { return e.status(); }
    }

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempFile("techflow-demo", ".json");
        Files.delete(tmp);
        TechFlowApi api = new TechFlowApi(new DemoTransport(tmp), new RequestLog());

        Object parsed = Json.parse("{\"a\":[1,2.5,\"x\\u00e9\\n\"],\"b\":null,\"c\":true}");
        check(Json.stringify(parsed).equals("{\"a\":[1,2.5,\"xé\\n\"],\"b\":null,\"c\":true}"), "JSON vai e volta sem perdas");

        check(statusOf(() -> api.projects()) == 401, "sem login a API responde 401");
        check(statusOf(() -> api.login("demo@techflow.dev", "errada123")) == 401, "senha errada responde 401");
        api.login(DemoTransport.DEMO_EMAIL, DemoTransport.DEMO_PASSWORD);
        check(api.me().email().equals("demo@techflow.dev"), "login e usuário atual");

        List<Project> projects = api.projects();
        check(projects.size() == 2, "a conta demo tem 2 projetos");
        long projectId = projects.get(0).id();

        check(statusOf(() -> api.createProject("  ", null, ProjectStatus.PLANNED)) == 400, "projeto sem nome responde 400");
        Project created = api.createProject("Projeto de teste", "descrição", ProjectStatus.PLANNED);
        Task task = api.createTask(created.id(), "Primeira tarefa", null, Priority.HIGH, LocalDate.now());
        check(task.status() == TaskStatus.TODO, "tarefa nova começa em A fazer");
        check(api.changeStatus(task.id(), TaskStatus.DONE).status() == TaskStatus.DONE, "mudança de status");
        check(api.tasks(created.id()).size() == 1, "listagem de tarefas do projeto");
        api.deleteTask(task.id());
        check(api.tasks(created.id()).isEmpty(), "exclusão de tarefa");
        api.deleteProject(created.id());
        check(statusOf(() -> api.tasks(created.id())) == 404, "projeto excluído responde 404");

        TechFlowApi other = new TechFlowApi(new DemoTransport(tmp), new RequestLog());
        other.register("Outra pessoa", "outra@example.com", "Senha@123");
        check(other.projects().isEmpty(), "cada usuário vê só os próprios projetos");
        check(statusOf(() -> other.tasks(projectId)) == 404, "projeto de outro usuário responde 404");
        check(statusOf(() -> other.register("Outra", "outra@example.com", "Senha@123")) == 400, "e-mail repetido responde 400");
        check(statusOf(() -> other.register("X", "x@example.com", "curta")) == 400, "senha curta responde 400");

        check(!api.log().all().isEmpty(), "o console registrou as chamadas");
        Files.deleteIfExists(tmp);
        System.out.println("\n" + checks + " verificações passaram.");
    }
}
