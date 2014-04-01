package br.univali.portugol.util.jar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Luiz Fernando Noschang
 */
public final class Classes implements Iterable<Class>
{
    private final CarregadorJar carregadorJar;
    private final List<Class> classes;
    private final Map<String, List<Class>> mapaClasses;

    private final List<String> filtrosArquivo = new ArrayList<>();
    private final List<String> filtrosPacote = new ArrayList<>();
    private final List<String> filtrosHeranca = new ArrayList<>();

    Classes(CarregadorJar carregadorJar, List<Class> classes, Map<String, List<Class>> mapaClasses)
    {
        this.carregadorJar = carregadorJar;
        this.classes = classes;
        this.mapaClasses = mapaClasses;
    }

    public Classes dosArquivos(File... arquivos)
    {
        for (File arquivo : arquivos)
        {
            filtrosArquivo.add(arquivo.getAbsolutePath());
        }

        return this;
    }

    public Classes nosPacotes(String... pacotes)
    {
        filtrosPacote.addAll(Arrays.asList(pacotes));

        return this;
    }

    public Classes queEstendemOuImplementam(String... classes)
    {
        filtrosHeranca.addAll(Arrays.asList(classes));

        return this;
    }

    public Classes queEstendemOuImplementam(Class... classes)
    {
        for (Class classe : classes)
        {
            filtrosHeranca.add(classe.getName());
        }

        return this;
    }

    @Override
    public Iterator<Class> iterator()
    {
        List<Class> listaClasses = new ArrayList<>(classes);

        filtrarArquivos(listaClasses);
        filtrarPacotes(listaClasses);
        filtrarHeranca(listaClasses);

        return listaClasses.iterator();
    }

    private void filtrarArquivos(List<Class> listaClasses)
    {
        if (!filtrosArquivo.isEmpty())
        {
            for (String filtroArquivo : filtrosArquivo)
            {
                if (mapaClasses.containsKey(filtroArquivo))
                {
                    listaClasses.retainAll(mapaClasses.get(filtroArquivo));
                }
            }
        }
    }

    private void filtrarPacotes(List<Class> listaClasses)
    {
        if (!filtrosPacote.isEmpty())
        {
            List<Class> classesEncontradas = new ArrayList<>();

            for (String pacote : filtrosPacote)
            {
                for (Class classe : listaClasses)
                {
                    if (pacote.endsWith(".*"))
                    {
                        String prefixoPacote = pacote.substring(0, pacote.lastIndexOf(".*"));
                        
                        if (classe.getPackage().getName().startsWith(prefixoPacote))
                        {
                            classesEncontradas.add(classe);
                        }
                    }
                    else if (classe.getPackage().getName().equals(pacote))
                    {
                        classesEncontradas.add(classe);
                    }
                }
            }

            listaClasses.retainAll(classesEncontradas);
        }
    }

    private void filtrarHeranca(List<Class> listaClasses)
    {
        if (!filtrosHeranca.isEmpty())
        {
            List<Class> classesEncontradas = new ArrayList<>();

            for (String nomeClasse : filtrosHeranca)
            {
                try
                {
                    Class classeHerdada = carregadorJar.getCarregador().loadClass(nomeClasse);

                    for (Class classe : listaClasses)
                    {
                        if (classeHerdada.isAssignableFrom(classe))
                        {
                            classesEncontradas.add(classe);
                        }
                    }
                }
                catch (ClassNotFoundException excecao)
                {

                }
            }

            listaClasses.retainAll(classesEncontradas);
        }
    }

    public int quantidade()
    {
        List<Class> listaClasses = new ArrayList<>(classes);

        filtrarArquivos(listaClasses);
        filtrarPacotes(listaClasses);
        filtrarHeranca(listaClasses);

        return listaClasses.size();
    }
}
