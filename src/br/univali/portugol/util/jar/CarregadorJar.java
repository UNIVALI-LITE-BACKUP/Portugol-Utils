package br.univali.portugol.util.jar;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Carrega todas as classes de um arquivo JAR na memória. Possui metodos
 * utilitários para listar as classes existentes no JAR e procurar classes
 * que são instancias de otutras classes
 *
 * @author Luiz Fernando Noschang
 */
public final class CarregadorJar
{
    private static final Logger LOGGER = Logger.getLogger(CarregadorJar.class.getName());

    private final FileFilter filtroArquivoJar = new FiltroArquivoJar();
    private final List<File> arquivosJar = new ArrayList<>();

    private ClassLoader carregador;
    private boolean carregado = false;
    private Classes classesCarregadas;

    public CarregadorJar()
    {

    }

    public void incluirCaminho(File caminho)
    {
        if (caminho.exists())
        {
            if (caminho.isFile())
            {
                incluirArquivo(caminho);
            }
            else
            {
                incluirDiretorio(caminho);
            }
        }
        else
        {
            String mensagem = String.format("O caminho '%s' não existe", caminho.getAbsolutePath());
            FileNotFoundException excecao = new FileNotFoundException(mensagem);

            LOGGER.log(Level.INFO, "", excecao);
        }
    }

    private void incluirArquivo(File caminho)
    {
        if (filtroArquivoJar.accept(caminho))
        {
            arquivosJar.add(caminho);
        }
        else
        {
            String mensagem = String.format("O caminho '%s' não é um arquivo JAR nem um diretório", caminho.getAbsolutePath());
            IllegalArgumentException excecao = new IllegalArgumentException(mensagem);

            LOGGER.log(Level.INFO, mensagem, excecao);
        }
    }

    private void incluirDiretorio(File diretorio)
    {
        File[] entradas = diretorio.listFiles(filtroArquivoJar);

        if (entradas != null)
        {
            for (File entrada : entradas)
            {
                incluirCaminho(entrada);
            }
        }
    }

    public List<File> getArquivosJar()
    {
        return new ArrayList<>(arquivosJar);
    }

    public void carregar()
    {
        if (!carregado)
        {
            Map<String, List<Class>> mapaClasses = new HashMap<>();
            List<Class> todasClasses = new ArrayList<>();

            URL[] urls = obterURLs();
            carregador = new URLClassLoader(urls);

            for (File arquivoJar : arquivosJar)
            {
                List<String> nomesClasses = listarNomesClasses(arquivoJar);
                List<Class> classesJar = carregarClasses(carregador, nomesClasses);

                todasClasses.addAll(classesJar);
                mapaClasses.put(arquivoJar.getAbsolutePath(), classesJar);
            }

            classesCarregadas = new Classes(this, todasClasses, mapaClasses);
            carregado = true;
        }
    }

    public Classes listarClasses()
    {
        return classesCarregadas;
    }

    private URL[] obterURLs()
    {
        URL[] urls = new URL[arquivosJar.size()];

        for (int i = 0; i < urls.length; i++)
        {
            try
            {
                urls[i] = arquivosJar.get(i).toURI().toURL();
            }
            catch (MalformedURLException excecao)
            {
                LOGGER.log(Level.SEVERE, String.format("Erro ao converter o caminho '%s' para URL", arquivosJar.get(i).getAbsolutePath()), excecao);
            }
        }

        return urls;
    }

    private List<Class> carregarClasses(ClassLoader carregador, List<String> nomesClasses)
    {
        List<Class> classes = new ArrayList<>();

        for (String nomeClasse : nomesClasses)
        {
            try
            {
                classes.add(carregador.loadClass(nomeClasse));
            }
            catch (ClassNotFoundException excecao)
            {
                LOGGER.log(Level.SEVERE, String.format("Erro ao carregar a classe '%s'", nomeClasse));
            }
        }

        return classes;
    }

    private List<String> listarNomesClasses(File arquivoJar)
    {
        List<String> nomes = new ArrayList<>();

        try
        {
            JarInputStream jar = new JarInputStream(new FileInputStream(arquivoJar));
            JarEntry entradaJar;

            while ((entradaJar = jar.getNextJarEntry()) != null)
            {
                if (entradaJar.getName().toLowerCase().endsWith(".class"))
                {
                    String nome = entradaJar.getName();

                    nome = nome.substring(0, nome.toLowerCase().indexOf(".class"));
                    nome = nome.replace("/", ".");

                    nomes.add(nome);
                }
            }
        }
        catch (IOException excecao)
        {
            LOGGER.log(Level.SEVERE, String.format("Erro ao listar as classes do JAR '%s'", arquivoJar.getAbsolutePath()), excecao);
        }

        return nomes;
    }
    
    ClassLoader getCarregador()
    {
        return carregador;
    }

    private final class FiltroArquivoJar implements FileFilter
    {
        @Override
        public boolean accept(File pathname)
        {
            return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".jar");
        }
    }
}
