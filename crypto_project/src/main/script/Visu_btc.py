import psycopg2
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

try:
    # Configuration de la connexion à PostgreSQL
    conn = psycopg2.connect(
        dbname="Btc_db",
        user="postgres",
        password="1234",
        host="localhost",
        port="5432"
    )

    # Récupération des données dans un DataFrame
    query = "SELECT * FROM btc_data;"
    df = pd.read_sql(query, conn)

    # Fermeture de la connexion
    conn.close()

    # Affichage des premières lignes pour vérification
    print(df.head())

    # Conversion des colonnes de temps en datetime
    df['window_start'] = pd.to_datetime(df['window_start'])
    df['window_end'] = pd.to_datetime(df['window_end'])

    # Tracé des graphiques
    plt.figure(figsize=(12, 6))

    # Graphique de la moyenne du prix de clôture (avg_close)
    plt.subplot(2, 2, 1)
    sns.lineplot(x='window_start', y='avg_close', data=df)
    plt.title('Moyenne du prix de clôture')
    plt.xlabel('Temps')
    plt.ylabel('Moyenne (Close)')

    # Graphique de l'écart-type du prix de clôture (stddev_close)
    plt.subplot(2, 2, 2)
    sns.lineplot(x='window_start', y='stddev_close', data=df)
    plt.title('Écart-type du prix de clôture')
    plt.xlabel('Temps')
    plt.ylabel('Écart-type (Close)')

    # Graphique du prix de clôture minimum (min_close)
    plt.subplot(2, 2, 3)
    sns.lineplot(x='window_start', y='min_close', data=df)
    plt.title('Prix de clôture minimum')
    plt.xlabel('Temps')
    plt.ylabel('Minimum (Close)')

    # Graphique du prix de clôture maximum (max_close)
    plt.subplot(2, 2, 4)
    sns.lineplot(x='window_start', y='max_close', data=df)
    plt.title('Prix de clôture maximum')
    plt.xlabel('Temps')
    plt.ylabel('Maximum (Close)')

    # Ajustement de l'espacement entre les sous-graphiques
    plt.tight_layout()

    # Affichage des graphiques
    plt.show()

except psycopg2.Error as e:
    print(f"Erreur de connexion à la base de données: {e}")
except Exception as e:
    print(f"Une erreur s'est produite: {e}")