# PCD Assignment 03

Componenti:

- Manuel Andruccioli, manuel.andruccioli@studio.unibo.it
- Kelvin Olaiya, kelvinoluwada.olaiya@studio.unibo.it
- Giacomo Totaro, giacomo.totaro2@studio.unibo.it

## Part 1 - Actors

L'implementazione della soluzione è stata realizzata mediante il linguaggio *Scala*, in particolare la libreria Akka, con attori tipizzati e behaviors.

### CLI

La soluzione proposta per la versione CLI è ispirata alla strategia *divide et impera*, esplorando cartelle e sottocartelle in modo ricorsivo. Vengono utilizzati i seguenti attori:

- **Manager:** questo attore è il punto d'ingresso del sistema, che crea il primo primo *DirectoryAnalyzer* con il root path di partenza. Inoltre, si occupa di stampare a video il risultato finale, al momento della ricezione.
- **DirectoryAnalyzer:** viene creato questa tipologia di attore *per ogni* sottocartella trovata ricorsivamente e si occupa di aggregare i risultati dei figli, per poi inviarli al padre. Inoltre, crea un *FileAnalyzer*, che processa i file presenti nella cartella corrente.
- **FileAnalyzer:** ogni *DirectoryAnalyzer* crea un *FileAnalyzer*, al quale vengono inviati i path dei file da processare, inviando il risultato al padre.

