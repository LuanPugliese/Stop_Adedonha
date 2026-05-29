package server;

import rmi.IStopServer;
import rmi.IStopClient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StopServerImpl extends UnicastRemoteObject implements IStopServer {
    
    private Map<String, IStopClient> jogadores = new ConcurrentHashMap<>();
    private Map<String, List<String>> respostasDaRodada = new ConcurrentHashMap<>();
    private boolean rodadaAtiva = false;
    private char letraAtual;
    private int respostasRecebidas = 0;

    protected StopServerImpl() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registrarJogador(String nome, IStopClient cliente) throws RemoteException {
        jogadores.put(nome, cliente);
        System.out.println("-> " + nome + " entrou na sala do jogo.");
    }

    @Override
    public synchronized void iniciarJogo() throws RemoteException {
        if (jogadores.isEmpty()) {
            System.out.println("Nenhum jogador conectado para iniciar.");
            return;
        }
        
        Random r = new Random();
        letraAtual = (char) (r.nextInt(26) + 'A');
        rodadaAtiva = true;
        respostasRecebidas = 0;
        respostasDaRodada.clear();

        System.out.println("Rodada iniciada! Letra sorteada: " + letraAtual);

        for (IStopClient cliente : jogadores.values()) {
            cliente.iniciarRodada(letraAtual);
        }
    }

    @Override
    public synchronized void gritarStop(String nome) throws RemoteException {
        if (rodadaAtiva) {
            rodadaAtiva = false;
            System.out.println("O jogador " + nome + " gritou STOP!");
            for (IStopClient cliente : jogadores.values()) {
                cliente.pararJogo(nome);
            }
        }
    }

    @Override
    public synchronized void enviarRespostas(String nome, List<String> respostas) throws RemoteException {
        respostasDaRodada.put(nome, respostas);
        respostasRecebidas++;

        if (respostasRecebidas == jogadores.size()) {
            calcularEEnviarResultados();
        }
    }

    private void calcularEEnviarResultados() {
        StringBuilder sb = new StringBuilder("\n--- PLACAR DA RODADA (Letra " + letraAtual + ") ---\n");
        
        for (Map.Entry<String, List<String>> entry : respostasDaRodada.entrySet()) {
            String nome = entry.getKey();
            List<String> respostas = entry.getValue();
            int pontos = 0;
            
            for (String resp : respostas) {
                if (resp != null && !resp.trim().isEmpty() && resp.toUpperCase().charAt(0) == letraAtual) {
                    pontos += 10;
                }
            }
            sb.append("• ").append(nome).append(": ").append(pontos).append(" pontos. ")
              .append(respostas.toString()).append("\n");
        }

        System.out.println("Resultados processados e enviados para as máquinas clientes.");
        
        for (IStopClient cliente : jogadores.values()) {
            try {
                cliente.mostrarResultados(sb.toString());
            } catch (RemoteException e) {
                System.out.println("Erro ao enviar resultado para um cliente desconectado.");
            }
        }
    }

    // =========================================================================
    // MAIN DO SERVIDOR (Executar primeiro na máquina que vai hospedar o jogo)
    // =========================================================================
    public static void main(String[] args) {
        try {
            // Instancia o objeto do jogo
            StopServerImpl servidor = new StopServerImpl();
            
            // Cria o registro RMI na própria máquina (Porta padrão 1099)
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Registra o serviço com o nome "StopServer"
            registry.rebind("StopServer", servidor);
            
            System.out.println("==================================================");
            System.out.println(" SERVIDOR DE STOP ATIVO E ONLINE (Porta 1099) ");
            System.out.println("==================================================");
            System.out.println("Aguardando conexões dos jogadores nas outras máquinas...");
        } catch (Exception e) {
            System.err.println("Falha crítica ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
