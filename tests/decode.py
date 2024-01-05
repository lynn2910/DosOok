sequence = "0100100000000000"

# Inverser la séquence
sequence = sequence[::-1]

# Initialiser la somme à zéro
seq_sum = 0

# Parcourir chaque bit dans la séquence inversée
for i in range(len(sequence)):
    # Convertir le bit en entier
    bit = int(sequence[i])

    # Calculer la somme des puissances
    seq_sum += bit * (2 ** (len(sequence) - 1 - i))

print("La valeur décimale correspondante est :", seq_sum)
