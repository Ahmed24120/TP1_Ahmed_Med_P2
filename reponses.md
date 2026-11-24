# TP 1 — SOAP et WSDL
**Ahmed Med Lemin · 24120 · DSI · SupNum**  
Architecture SOA & Web Services · 2026–2027 · Travail individuel

## Étape 1 — Publier le service Java

**1. Quelles annotations exposent le service et sa faute métier ?**  
`@WebService` expose la classe `EmpruntService`. Sa méthode `emprunterLivre` déclare `throws LivreIndisponibleException`. L’annotation `@WebFault(name = "LivreIndisponible", …)` sur cette exception associe la faute à son élément XML. La méthode `getFaultInfo()` fournit les données métier à sérialiser dans le détail de la faute.

**2. Pourquoi un constructeur vide ?**  
Jakarta XML Binding (JAXB) doit pouvoir instancier les objets de données lorsqu’il lit du XML, puis remplir leurs champs. Le constructeur public sans argument de `Emprunt` et de `LivreIndisponibleInfo` lui permet de procéder ainsi. Le constructeur à deux arguments sert au code métier.

## Étape 2 — Lire le WSDL généré

**1. Où appeler le service et quels messages échanger ?**  
Dans `service`, le port `EmpruntPort` du service `EmpruntService` donne l’adresse `http://localhost:8080/bib/EmpruntService`. Le suffixe `?wsdl` sert à consulter le contrat. Le port renvoie au binding `EmpruntPortBinding`, qui renvoie au `portType` nommé `EmpruntService`. Les noms ci-dessous sont ceux des éléments `wsdl:message` effectivement publiés en partie A, dans l’espace `http://supnum.mr/bib`.

| Opération du portType | Message d’entrée | Message de sortie | Faute déclarée |
|---|---|---|---|
| `emprunterLivre` | `emprunterLivre` | `emprunterLivreResponse` | `LivreIndisponibleException` |
| `rendreLivre` | `rendreLivre` | `rendreLivreResponse` | Aucune dans le WSDL |

**2. Quel binding ?**  
`soap:binding` indique `style="document"` et le transport SOAP sur HTTP. Les `soap:body` indiquent `use="literal"` en entrée et en sortie. L’espace de noms de l’extension WSDL est `http://schemas.xmlsoap.org/wsdl/soap/` et celui de l’enveloppe est `http://schemas.xmlsoap.org/soap/envelope/`. Le service utilise SOAP 1.1.

**3. Quels types ont été générés ?**  
Le WSDL importe le schéma à `http://localhost:8080/bib/EmpruntService?xsd=1`. Dans ce XSD, `isbn` et `dateRetour` sont tous les deux des `xs:string`, comme leurs champs Java de type `String`. La regex ISBN se trouve dans le corps de la méthode Java. Le schéma généré ne contient donc aucune facette `xs:pattern` pour cet ISBN.

**4. Les paramètres sont-ils préfixés dans SoapUI ?**  
Le modèle importé dans SoapUI donne une opération qualifiée, `<bib:emprunterLivre>`, et deux enfants sans préfixe ni espace de noms par défaut, `<idAdherent>` et `<isbn>`. Le XSD de A laisse les éléments locaux non qualifiés. L’envoi de `A-117` et `978-1-234-5678-9` depuis une requête générée par SoapUI a renvoyé HTTP 200 et l’emprunt `E-2026-0812`.

**5. Que lit-on dans Raw ?**  
La première ligne, par exemple `POST /bib/EmpruntService HTTP/1.1`, et les en-têtes `Content-Type` et `SOAPAction` appartiennent à HTTP. HTTP transporte le message et fournit le statut de la réponse. Après la ligne vide, XML donne la syntaxe des éléments, de leurs attributs et de leurs espaces de noms. SOAP donne un sens à `Envelope`, au `Header` facultatif, au `Body` et à la `Fault`. Le WSDL décrit à l’avance l’adresse, les opérations et le format des messages. SoapUI l’utilise pour générer sa requête.

## Étape 3 — Construire et envoyer les messages

**1. Pourquoi HTTP POST ?**  
La liaison SOAP 1.1 sur HTTP utilisée ici envoie l’enveloppe dans le corps d’un POST. Le serveur identifie l’opération à partir du message SOAP. Une opération de consultation suivrait le même mécanisme. La lecture du document WSDL se fait, elle, avec un GET.

**2. Quel effet a le jeton facultatif ?**  
`xml/01-emprunter.xml` contient le bloc `<bib:jeton>abc123</bib:jeton>`. Aucun gestionnaire ne traite ce bloc et l’absence de `soap:mustUnderstand` équivaut à la valeur `0`. Le serveur a traité la demande. Après l’essai SoapUI, cette seconde demande a obtenu HTTP 200, le numéro `E-2026-0813` et la date de retour `2026-12-08` lors de l’exécution du 24 novembre 2026. Sur un serveur fraîchement démarré, le premier numéro est `E-2026-0812`. Renvoyer le même ISBN sur cette instance déclenche ensuite la faute de livre indisponible.

**3. Que répond le service à l’enveloppe SOAP 1.2 ?**  
Avec `xml/02-emprunter-12.xml` et le même en-tête HTTP `Content-Type: text/xml; charset=utf-8`, le résultat observé est HTTP 500 et une faute SOAP 1.1 de code `S:VersionMismatch`. Le lecteur attend l’espace d’enveloppe SOAP 1.1, mais trouve `http://www.w3.org/2003/05/soap-envelope`. Il s’arrête avant d’appeler la méthode Java.

**4. Quelles sont les cinq corrections ?**  
Le message original est dans `xml/04-rendre-original.xml.txt`. La version corrigée, `xml/04-rendre-corrige.xml`, a obtenu HTTP 200 avec `<penalite>0</penalite>`.

| Nº | Erreur observée dans l’original | Correction |
|---|---|---|
| 1 | L’ouverture `soap:envelope` a une minuscule et ne correspond pas à la fermeture. | Écrire `<soap:Envelope>`. XML distingue majuscules et minuscules. |
| 2 | `bib` désigne `http://supnum.mr/bibliotheque`. | Déclarer `xmlns:bib="http://supnum.mr/bib"` pour que le serveur reconnaisse l’opération. |
| 3 | L’opération commence par `<bib:rendreLivre>` et finit par `</rendreLivre>`. | Fermer avec `</bib:rendreLivre>`. |
| 4 | Le QName `bib:numero` a un nom local et un espace de noms incorrects. | Utiliser `<numeroEmprunt>E-2026-0812</numeroEmprunt>`, sans préfixe ni espace par défaut dans A. |
| 5 | Le `Header` est placé après le `Body`. | Placer le `Header` avant le `Body`. |

Sur l’original, Metro s’arrête dès `soap:envelope` et renvoie HTTP 500. Pour distinguer les effets, chaque anomalie a aussi été isolée sur le message corrigé. Avec la méthode initiale de l’étape 3 qui renvoie toujours zéro, un mauvais nom de paramètre ou un mauvais préfixe a donné HTTP 200 et zéro. Le paramètre attendu reste absent et la méthode initiale ne le consulte pas. Après ajout du contrôle de l’étape 4, une valeur absente provoque une faute `Client`. Le `Header` placé après le `Body` a lui aussi été toléré par cette version de Metro, bien que cet ordre soit contraire à SOAP. Un espace de noms erroné sur l’opération empêche son identification.

## Étape 4 — Déclencher et construire des Faults

**1. Quels codes et libellés reçoit-on ?**  
Les réponses suivantes proviennent du service A. Dans les réponses de Metro, le préfixe `S` désigne l’espace de noms de l’enveloppe SOAP 1.1.

| Cas et fichier | HTTP | faultcode | faultstring |
|---|---:|---|---|
| ISBN incomplet `978-2-10` · `05-isbn-invalide.xml` | 500 | `S:Client` | `ISBN mal formé : 978-2-10` |
| ISBN déjà sorti `978-2-10-074444-4` · `06-livre-indisponible.xml` | 500 | `S:Server` | `Livre indisponible` |
| Retour `X-0812` · `09-retour-invalide.xml` | 500 | `S:Client` | `Numéro d'emprunt inconnu : X-0812` |

**2. Que contient le détail de la faute métier ?**  
Voici le contenu reçu en partie A, avec un préfixe lisible et les espaces de noms conservés.

```xml
<detail>
  <bib:LivreIndisponible xmlns:bib="http://supnum.mr/bib">
    <isbn>978-2-10-074444-4</isbn>
    <retourPrevu>2026-12-02</retourPrevu>
  </bib:LivreIndisponible>
</detail>
```

Dans le WSDL, le `fault` de l’opération pointe vers le message `LivreIndisponibleException`. Sa partie `fault` référence l’élément global `LivreIndisponible`. Dans `?xsd=1`, cet élément a le type complexe `livreIndisponibleInfo`, dont la séquence contient `isbn` et `retourPrevu`.

**3. Comment interpréter le code choisi par JAX-WS ?**  
Metro a produit `Server` pour l’exception métier déclarée. La requête est correctement formée et le client ne peut pas rendre le livre disponible. Je garderais `Server` dans ce TP, car le même message pourra réussir après restitution du livre. Le détail typé permet au client d’afficher la date prévue et de distinguer ce cas métier d’une panne technique.

**4. Que change mustUnderstand ?**  
Avec `soap:mustUnderstand="1"` sur `bib:jeton`, Metro renvoie HTTP 500 et `S:MustUnderstand`. Le destinataire ne sait pas traiter ce bloc obligatoire. Avec `"0"`, il peut l’ignorer. Les fichiers `07-jeton-obligatoire.xml` et `08-jeton-facultatif.xml` utilisent le même livre disponible. Envoyés dans cet ordre, ils ont donné respectivement une faute et un emprunt réussi (HTTP 200). Le premier essai n’a donc pas enregistré d’emprunt.

**5. Quelle différence avec la Fault SOAP 1.2 écrite à la main ?**  
La faute 1.1 reçue utilise `faultcode`, `faultstring` et éventuellement `detail`. Dans `03-fault-sender-12.xml`, la faute 1.2 contient `soap:Code/soap:Value` égal à `soap:Sender`, un texte français dans `soap:Reason/soap:Text` avec `xml:lang="fr"`, et `soap:Detail` avec l’ISBN rejeté. Les éléments de structure de la faute 1.2 sont qualifiés. `Sender` correspond ici à `Client` et `Receiver` à `Server`. La commande `xmllint --noout xml/03-fault-sender-12.xml` s’est terminée avec le code 0. Elle vérifie que le XML est bien formé. Cet exemple est une réponse construite pour un service 1.2.

## Étape 5 — Générer l’interface depuis le WSDL

**1. Quelle opération, quelle faute, quelle action ?**  
Dans `EmpruntPortType`, l’unique opération est `emprunterLivre`. Elle relie `emprunterLivreRequest` à `emprunterLivreResponse`. La faute d’opération nommée `LivreIndisponible` référence le message `bib:LivreIndisponibleFault`. Dans `EmpruntSoapBinding`, l’action à envoyer est `SOAPAction: "http://supnum.mr/bib/emprunterLivre"`.

**2. Quelle signature wsimport a-t-il produite ?**  
Dans `target/generated-sources/wsimport/mr/supnum/bib/contrat/EmpruntPortType.java`, la signature Java, annotations omises, est la suivante.

```java
public EmpruntType emprunterLivre(String idAdherent, String isbn)
        throws LivreIndisponibleFault;
```

**3. Quel type Java pour dateRetour ?**  
Le champ `dateRetour` de `EmpruntType` est un `javax.xml.datatype.XMLGregorianCalendar`, avec le type XML `date`. Il provient du `xs:date` du contrat. En partie A, `Emprunt.dateRetour` était un `String` et le XSD annonçait `xs:string`. Ainsi, B exprime une contrainte de date même si les deux réponses affichent la même forme `AAAA-MM-JJ`.

**4. D’où vient le nom de l’exception ?**  
Le message WSDL référencé dans `<wsdl:fault name="LivreIndisponible" message="bib:LivreIndisponibleFault"/>` s’appelle `LivreIndisponibleFault`. Dans le code généré, `LivreIndisponibleFault` est l’exception Java et `LivreIndisponible` est la classe de données renvoyée par `getFaultInfo()`.

## Étape 6 — Implémenter et comparer

**1. Quel WSDL est publié ?**  
Avec `wsdlLocation = "wsdl/EmpruntService.wsdl"` dans `@WebService`, le contrat publié reprend le WSDL fourni. On retrouve le binding `EmpruntSoapBinding` et le schéma intégré, notamment le motif `97[89]-\d-\d{2,5}-\d{2,6}-\d` dans `IsbnType` ainsi que `xs:date` pour `dateRetour` et `retourPrevu`. Metro ajoute un commentaire de publication.

**2. Qu’a-t-il fallu modifier dans la requête A ?**  
Le contrat B impose `elementFormDefault="qualified"`. L’opération et ses deux enfants appartiennent donc à `http://supnum.mr/bib`. Dans `10-contrat-emprunter.xml`, les enfants sont `<bib:idAdherent>` et `<bib:isbn>`. L’en-tête HTTP utilise l’action explicite de l’étape 5. Sur le service livré avec validation active, le fichier A accompagné de la bonne action a donné HTTP 500 sur `idAdherent` non qualifié. Le fichier B corrigé a donné HTTP 200. Le même essai avec le livre déjà sorti (`11-contrat-indisponible.xml`) a donné `S:Server` et un détail métier contenant l’ISBN et la date prévue. Sans validation, le fichier A peut donner un succès trompeur, car la méthode d’exemple renvoie un emprunt même si ses arguments sont absents.

**3. Que change @SchemaValidation pour un ISBN incomplet ?**  
Les deux essais utilisent exactement `12-contrat-isbn-invalide.xml`, avec des paramètres qualifiés et l’ISBN `978-2-10`.

| Configuration du même service B | Réponse observée | Explication |
|---|---|---|
| Contrat fourni, sans `@SchemaValidation` | HTTP 200, emprunt `E-2026-0812` | La méthode distingue uniquement l’ISBN déjà sorti. Le type Java `String` n’applique pas la regex. |
| Contrat fourni, avec `@SchemaValidation` | HTTP 500, `S:Client` | Avant l’appel Java, le validateur signale `cvc-pattern-valid`, la valeur `978-2-10` et le type `IsbnType`. |

**4. Que se passe-t-il quand on retire wsdlLocation de @WebService ?**  
Test réalisé en conservant l’interface générée, le WSDL dans les ressources et `@SchemaValidation`. Après redémarrage, Metro a régénéré le contrat depuis Java. Le binding est devenu `EmpruntPortBinding` et le schéma est importé depuis `?xsd=1`. Dans ce XSD, `isbn` est un simple `xs:string` sans motif, tandis que `dateRetour` reste `xs:date` grâce aux annotations JAXB générées. Le même ISBN incomplet a alors obtenu HTTP 200, même avec la validation active. La version livrée rétablit `wsdlLocation` pour publier le contrat demandé.

## Conclusion — Choix de l’approche

**1. Deux différences observées entre les contrats A et B.**

| Point comparé | A · code-first | B · contract-first |
|---|---|---|
| Opérations exposées | `emprunterLivre` et `rendreLivre` | `emprunterLivre` uniquement |
| Type et contrôle de l’ISBN annoncés | `xs:string` et aucun motif dans le contrat | `bib:IsbnType` restreint par `xs:pattern` |

**2. Que se passe-t-il si l’on renomme dateRetour dans Java ?**  
En A, sans annotation qui fixe son nom XML, le renommage du champ modifie l’élément du XSD à la prochaine publication. Un ancien client qui lit `dateRetour` risque de perdre la valeur ou de rejeter la réponse. En B, le WSDL fourni reste la référence. Renommer une variable interne laisse le contrat intact. Pour renommer l’élément public, il faudrait modifier le WSDL, régénérer les classes et organiser la transition avec les clients.

**3. Quelle approche pour Paiement et pour un prototype ?**  
Pour Paiement, je choisirais le contract-first. Scolarité (.NET), Bibliothèque (PHP) et le portail (Node.js) peuvent valider ensemble les formats des montants, les opérations et les fautes avant de développer chacun leur client. Pour un prototype interne de deux jours, je choisirais le code-first afin de publier rapidement les premières opérations Java. Avant qu’un autre système en dépende en production, je figerais une version du WSDL.

## Rejouer le TP sous Windows (Invite de commandes)

Utiliser JDK 17 et Maven. Ouvrir deux fenêtres `cmd` à la racine du dossier extrait, là où se trouve ce fichier. Vérifier avec `mvn -version` que Maven utilise Java 17. Le premier lancement télécharge les dépendances.

**Serveur A, dans la première fenêtre**

```bat
cd emprunt-codefirst
mvn compile exec:java
```

**Essai SoapUI et requêtes A, dans la deuxième fenêtre**  
Dans SoapUI, choisir **File > New SOAP Project**, saisir l’adresse obtenue en ajoutant `?wsdl` à l’URL du service, puis ouvrir **emprunterLivre > Request 1**. Remplir `A-117` et `978-1-234-5678-9`, envoyer la requête et consulter **Raw**. Dans la deuxième fenêtre, les commandes suivantes reproduisent ensuite les cas A. Le fichier `03` est une réponse à contrôler localement.

```bat
set "URL=http://localhost:8080/bib/EmpruntService"
curl.exe -i -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"\"" --data-binary "@xml/01-emprunter.xml" "%URL%"
```

Pour les autres essais A, reprendre la dernière commande en remplaçant le fichier après `@` par le cas voulu dans `xml/`. Pour tester les erreurs de l’étape 3, envoyer d’abord `04-rendre-original.xml.txt`, puis `04-rendre-corrige.xml`. Pour contrôler la Fault SOAP 1.2 avec l’outil de l’énoncé, exécuter `xmllint --noout xml/03-fault-sender-12.xml` si `xmllint` est installé.

**Serveur B, dans la première fenêtre**  
Arrêter A avec **Ctrl+C**. Depuis le dossier `emprunt-codefirst`, exécuter les commandes suivantes. Les deux serveurs utilisent le même port 8080.

```bat
cd ..\emprunt-contractfirst
mvn generate-sources
mvn compile exec:java
```

**Requêtes B, dans la deuxième fenêtre**  
La variable `URL` définie plus haut reste disponible dans cette fenêtre.

```bat
curl.exe -i -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"http://supnum.mr/bib/emprunterLivre\"" --data-binary "@xml/10-contrat-emprunter.xml" "%URL%"
curl.exe -i -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"http://supnum.mr/bib/emprunterLivre\"" --data-binary "@xml/11-contrat-indisponible.xml" "%URL%"
curl.exe -i -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"http://supnum.mr/bib/emprunterLivre\"" --data-binary "@xml/12-contrat-isbn-invalide.xml" "%URL%"
```

Pour reproduire les comparaisons de l’étape 6, arrêter B avant chaque modification de `EmpruntServiceImpl.java`, relancer `mvn compile exec:java`, puis renvoyer le fichier `12`. Pour le premier essai, retirer uniquement `@SchemaValidation`. Pour le second, rétablir cette annotation et retirer uniquement le paramètre `wsdlLocation` de `@WebService`, avec la virgule qui le précède. Rétablir ensuite le fichier d’origine. Le répertoire `target/` se reconstruit automatiquement.

**Conditions de vérification.** Les deux projets ont été compilés et lancés sous Linux avec OpenJDK 17, Maven 3.9.9 et Metro 4.0.2. La requête de l’étape 2 a été importée, générée et envoyée avec SoapUI Open Source 5.8.0 en mode sans interface graphique. Les autres échanges HTTP ont été exécutés avec curl. Les commandes Windows ci-dessus permettent de reproduire ces essais sur le poste de cours.
