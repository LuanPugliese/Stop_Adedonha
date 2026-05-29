package client;

import rmi.IStopServer;
import rmi.IStopClient;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StopGameGUI extends JFrame implements IStopClient {
    
    private static final String IP_SERVIDOR = "192.168.0.33"; 
    private static final int PORTA_SERVIDOR = 1099;
    private static final String IP_MEU_COMPUTADOR = "192.168.0.33"; 

    private IStopServer servidor;
    private String meuNome;
    
    // Componentes Visuais
    private JLabel lblStatus;
    private JLabel lblLetra;
    private JTextField txtNome, txtCidade, txtAnimal;
    private JButton btnStop, btnStart;
    private JTextArea txtResultados;
    
    // Componentes Novos para a Área de Votação
    private JPanel painelVotacao;
    private Map<String, JComboBox<String>> listaDeVotosComponentes = new HashMap<>();
    private JButton btnEnviarVotos;
    
    public StopGameGUI(String nome, IStopServer servidor) throws RemoteException {
        this.meuNome = nome;
        this.servidor = servidor;
        
        UnicastRemoteObject.exportObject(this, 0);
        servidor.registrarJogador(nome, this);
        
        configurarInterface();
    }
    
    private void configurarInterface() {
        setTitle("STOP Votação RMI - Jogador: " + meuNome);
        setSize(480, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel painelTopo = new JPanel(new GridLayout(2, 1));
        lblStatus = new JLabel("Conectado! Aguardando o início...", SwingConstants.CENTER);
        lblLetra = new JLabel("Letra: ?", SwingConstants.CENTER);
        lblLetra.setFont(new Font("Arial", Font.BOLD, 36));
        lblLetra.setForeground(new Color(43, 108, 176));
        painelTopo.add(lblStatus);
        painelTopo.add(lblLetra);
        add(painelTopo, BorderLayout.NORTH);
        
        JPanel painelCentro = new JPanel(new GridLayout(3, 2, 5, 15));
        painelCentro.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        painelCentro.add(new JLabel("Nome Próprio:"));
        txtNome = new JTextField(); painelCentro.add(txtNome);
        painelCentro.add(new JLabel("Cidade/Estado/País:"));
        txtCidade = new JTextField(); painelCentro.add(txtCidade);
        painelCentro.add(new JLabel("Animal:"));
        txtAnimal = new JTextField(); painelCentro.add(txtAnimal);
        add(painelCentro, BorderLayout.CENTER);
        
        JPanel painelBase = new JPanel(new BorderLayout());
        JPanel painelBotoes = new JPanel();
        btnStart = new JButton("Nova Rodada");
        btnStop = new JButton("STOP!");
        btnStop.setBackground(new Color(229, 62, 62));
        btnStop.setForeground(Color.WHITE);
        painelBotoes.add(btnStart); painelBotoes.add(btnStop);
        painelBase.add(painelBotoes, BorderLayout.NORTH);
        
        // Área dinâmica: Pode mostrar os resultados ou a aba de votação
        painelVotacao = new JPanel();
        painelVotacao.setLayout(new BoxLayout(painelVotacao, BoxLayout.Y_AXIS));
        
        txtResultados = new JTextArea(10, 30);
        txtResultados.setEditable(false);
        txtResultados.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollArea = new JScrollPane(txtResultados);
        scrollArea.setBorder(BorderFactory.createTitledBorder("Painel do Jogo / Votação"));
        painelBase.add(scrollArea, BorderLayout.CENTER);
        add(painelBase, BorderLayout.SOUTH);
        
        bloquearCampos();
        
        btnStart.addActionListener(e -> {
            try { servidor.iniciarJogo(); } catch (RemoteException ex) { ex.printStackTrace(); }
        });
        
        btnStop.addActionListener(e -> {
            try { servidor.gritarStop(meuNome); } catch (RemoteException ex) { ex.printStackTrace(); }
        });
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void bloquearCampos() {
        txtNome.setEnabled(false); txtCidade.setEnabled(false); txtAnimal.setEnabled(false);
        btnStop.setEnabled(false);
    }
    
    private void liberarCampos() {
        txtNome.setEnabled(true); txtCidade.setEnabled(true); txtAnimal.setEnabled(true);
        btnStop.setEnabled(true);
        txtNome.setText(""); txtCidade.setText(""); txtAnimal.setText("");
    }

    // ==========================================
    // MÉTODOS REMOTOS (Callbacks do Servidor)
    // ==========================================

    @Override
    public void iniciarRodada(char letra) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("RODADA ATIVA! Digite rápido!");
            lblLetra.setText("Letra: " + letra);
            liberarCampos();
            txtResultados.setText("Jogo em andamento...");
            // Limpa resíduos da votação anterior se havor
            painelVotacao.removeAll();
        });
    }

    @Override
    public void pararJogo(String quemParou) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            bloquearCampos();
            lblStatus.setText("FIM DA RODADA! Recolhendo dados para votação...");
            
            List<String> minhasRespostas = new ArrayList<>();
            minhasRespostas.add(txtNome.getText());
            minhasRespostas.add(txtCidade.getText());
            minhasRespostas.add(txtAnimal.getText());
            
            try { servidor.enviarRespostas(meuNome, minhasRespostas); } catch (RemoteException e) { e.printStackTrace(); }
        });
    }

    @Override
    public void abrirFaseVotacao(Map<String, Map<String, String>> todasRespostas) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("FASE DE VOTAÇÃO: Avalie se as palavras existem!");
            txtResultados.setVisible(false); // Esconde a área de texto limpa
            
            painelVotacao.removeAll();
            listaDeVotosComponentes.clear();
            
            painelVotacao.add(new JLabel("Selecione S (Sim) se a palavra existe ou N (Não):"));
            
            // Cria dinamicamente linhas com as palavras de todos os jogadores para votar
            for (Map.Entry<String, Map<String, String>> jogadorEntry : todasRespostas.entrySet()) {
                String jogador = jogadorEntry.getKey();
                
                JPanel linhaJogador = new JPanel(new FlowLayout(FlowLayout.LEFT));
                linhaJogador.add(new JLabel("● [" + jogador + "]: "));
                
                for (Map.Entry<String, String> cat : jogadorEntry.getValue().entrySet()) {
                    String palavra = cat.getValue().trim();
                    if (!palavra.isEmpty()) {
                        linhaJogador.add(new JLabel(cat.getKey() + ": " + palavra));
                        
                        // Caixa de seleção com opções S (Sim) e N (Não)
                        JComboBox<String> comboVoto = new JComboBox<>(new String[]{"S", "N"});
                        linhaJogador.add(comboVoto);
                        
                        // Mapeia o componente à palavra específica para ler o voto depois
                        listaDeVotosComponentes.put(palavra, comboVoto);
                    }
                }
                painelVotacao.add(linhaJogador);
            }
            
            btnEnviarVotos = new JButton("Enviar Meus Votos");
            btnEnviarVotos.addActionListener(e -> {
                Map<String, Boolean> meusVotosLidos = new HashMap<>();
                for (Map.Entry<String, JComboBox<String>> item : listaDeVotosComponentes.entrySet()) {
                    String palavra = item.getKey();
                    String escolha = (String) item.getValue().getSelectedItem();
                    meusVotosLidos.put(palavra, escolha.equals("S"));
                }
                
                try {
                    btnEnviarVotos.setEnabled(false);
                    lblStatus.setText("Votos enviados! Aguardando os demais jogadores...");
                    servidor.enviarVotos(meuNome, meusVotosLidos);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            });
            
            painelVotacao.add(btnEnviarVotos);
            
            // Coloca o painel visual de votação no lugar da caixa de texto antiga
            JScrollPane scroll = (JScrollPane) txtResultados.getParent().getParent();
            scroll.setViewportView(painelVotacao);
            painelVotacao.revalidate();
            painelVotacao.repaint();
        });
    }

    @Override
    public void mostrarResultados(String resultados) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            // Restaura a caixa de texto para exibir o placar detalhado enviado pelo servidor
            JScrollPane scroll = (JScrollPane) painelVotacao.getParent().getParent();
            scroll.setViewportView(txtResultados);
            txtResultados.setVisible(true);
            txtResultados.setText(resultados);
            
            lblStatus.setText("Rodada encerrada. Próxima disponível.");
        });
    }

    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", IP_MEU_COMPUTADOR);
            String nome = JOptionPane.showInputDialog(null, "Digite seu nome:", "Stop RMI", JOptionPane.QUESTION_MESSAGE);
            if (nome == null || nome.trim().isEmpty()) System.exit(0);

            Registry registro = LocateRegistry.getRegistry(IP_SERVIDOR, PORTA_SERVIDOR);
            IStopServer servidorRemoto = (IStopServer) registro.lookup("StopService");
            new StopGameGUI(nome, servidorRemoto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}