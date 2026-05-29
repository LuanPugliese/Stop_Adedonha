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
    
    // Guarda as respostas: Jogador -> (Categoria -> Palavra)
    private Map<String, Map<String, String>> respostasDaRodada = new ConcurrentHashMap<>();
    
    // Guarda a contagem de votos positivos para cada palavra digitada
    private Map<String, Integer> votosPositivosPalavra = new ConcurrentHashMap<>();
    
    private boolean rodadaAtiva = false;
    private char letraAtual;
    private int respostasRecebidas = 0;
    private int votosRecebidos = 0;

    protected StopServerImpl() throws RemoteException {
        super();
    }

    @Override
    public synchronized void registrarJogador(String nome, IStopClient cliente) throws RemoteException {
        jogadores.put(nome, cliente);
        System.out.println("-> " + nome + " conectou-se ao servidor.");
    }

    @Override
    public synchronized void iniciarJogo() throws RemoteException {
        if (jogadores.isEmpty()) return;
        
        Random r = new Random();
        letraAtual = (char) (r.nextInt(26) + 'A');
        rodadaAtiva = true;
        respostasRecebidas = 0;
        votosRecebidos = 0;
        respostasDaRodada.clear();
        votosPositivosPalavra.clear();

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
        Map<String, String> categorias = new HashMap<>();
        categorias.put("Nome", respostas.get(0));
        categorias.put("Cidade", respostas.get(1));
        categorias.put("Animal", respostas.get(2));
        
        respostasDaRodada.put(nome, categorias);
        respostasRecebidas++;

        // Quando todos enviarem as respostas, envia o painel de votação para todos
        if (respostasRecebidas == jogadores.size()) {
            System.out.println("Todas as respostas recebidas. Iniciando fase de votação...");
            for (IStopClient cliente : jogadores.values()) {
                cliente.abrirFaseVotacao(respostasDaRodada);
            }
        }
    }

    @Override
    public synchronized void enviarVotos(String nomeJogador, Map<String, Boolean> votos) throws RemoteException {
        // Computa os votos recebidos deste jogador
        for (Map.Entry<String, Boolean> voto : votos.entrySet()) {
            String palavra = voto.getKey().toUpperCase().trim();
            boolean aceita = voto.getValue();
            
            if (aceita) {
                votosPositivosPalavra.put(palavra, votosPositivosPalavra.getOrDefault(palavra, 0) + 1);
            }
        }
        
        votosRecebidos++;
        
        // Quando todos os jogadores terminarem de votar, calcula o placar final
        if (votosRecebidos == jogadores.size()) {
            calcularResultadosFinais();
        }
    }

    private void calcularResultadosFinais() {
        StringBuilder sb = new StringBuilder("\n--- PLACAR FINAL REGULADO POR VOTAÇÃO (Letra " + letraAtual + ") ---\n");
        int metadeDosJogadores = jogadores.size() / 2;

        for (Map.Entry<String, Map<String, String>> entry : respostasDaRodada.entrySet()) {
            String jogador = entry.getKey();
            Map<String, String> palavrasDoJogador = entry.getValue();
            int pontosDoJogador = 0;
            
            sb.append("• ").append(jogador).append(" escreveu:\n");
            
            for (Map.Entry<String, String> categoria : palavrasDoJogador.entrySet()) {
                String palavra = categoria.getValue().trim();
                String catNome = categoria.getKey();
                
                if (palavra.isEmpty()) {
                    sb.append("   - ").append(catNome).append(": [Em Branco] -> 0 pts\n");
                    continue;
                }
                
                // Verifica se a palavra começa com a letra correta
                boolean letraValida = palavra.toUpperCase().charAt(0) == letraAtual;
                
                // Pega quantos votos positivos essa palavra teve
                int votosAprovados = votosPositivosPalavra.getOrDefault(palavra.toUpperCase(), 0);
                
                // Regra da maioria: A palavra é válida se a maioria dos jogadores votou "Sim" (S)
                boolean palavraExiste = votosAprovados > metadeDosJogadores;
                
                if (letraValida && palavraExiste) {
                    pontosDoJogador += 10;
                    sb.append("   - ").append(catNome).append(": ").append(palavra)
                      .append(" (Aprovada por ").append(votosAprovados).append(" jogadores) -> 10 pts\n");
                } else {
                    String motivo = !letraValida ? "Letra Incorreta" : "Palavra Rejeitada em Votação (" + votosAprovados + " votos S)";
                    sb.append("   - ").append(catNome).append(": ").append(palavra)
                      .append(" [INVÁLIDA - ").append(motivo).append("] -> 0 pts\n");
                }
            }
            sb.append("  Total do jogador: ").append(pontosDoJogador).append(" pontos.\n\n");
        }

        System.out.println("Resultados finais validados enviados aos clientes.");
        
        for (IStopClient cliente : jogadores.values()) {
            try {
                cliente.mostrarResultados(sb.toString());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            String ipDoServidor = "192.168.0.33"; 
            int porta = 1099;
            
            System.setProperty("java.rmi.server.hostname", ipDoServidor);
            StopServerImpl servidor = new StopServerImpl();
            Registry registry = LocateRegistry.createRegistry(porta);
            registry.rebind("StopService", servidor);
            
            System.out.println("==================================================");
            System.out.println(" SERVIDOR STOP VOTAÇÃO ONLINE NO IP: " + ipDoServidor);
            System.out.println("==================================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}