package com.alipe.techflow.desktop.ui;

import com.alipe.techflow.desktop.net.ApiException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/** Roda chamadas de rede fora da thread da interface e devolve o resultado nela. */
public final class Async {
    private Async() {}

    public static <T> void run(Callable<T> job, Consumer<T> onOk, Consumer<ApiException> onError) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return job.call(); }

            @Override protected void done() {
                try {
                    onOk.accept(get());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    onError.accept(cause instanceof ApiException a ? a
                            : new ApiException(0, cause == null ? "Erro inesperado" : String.valueOf(cause.getMessage())));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
