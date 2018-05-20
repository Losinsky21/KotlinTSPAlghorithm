package com.arturlosin.TSP

import java.io.*
import java.util.*
import java.util.stream.IntStream

var FILE_LENGTH = 0
var bestResult = Integer.MAX_VALUE

val fileName = "pr76"
//val fileName = "kroC100"
//val fileName = "eil51"
//val fileName = "berlin52"
//val fileName = "att48"
//val fileName = "a280"

fun main(args: Array<String>) {

    var matrix = ReadFile("$fileName.txt")
    var bestCandidate = IntArray(FILE_LENGTH)

    //creating population
    var population = RandomPopulationGenerator(FILE_LENGTH, 40)

    for (counter in 0..99999)
    {
        val tournamentResult = Tournament(population, matrix,4)
        //val rouletteResult = Roulette(population, matrix,4) - wykonanie ruletki zamiast turnieju
        val pmxResult = CalculatePMX(tournamentResult,30 )
        val mutationResult = Mutate(70, pmxResult)

        population = mutationResult.clone()

        // najlepszy osobnik z kazdego wykonania sie petli
        when {
            ReturnBestSubjectLength(population, matrix) < bestResult ->
            {
                bestResult = ReturnBestSubjectLength(population, matrix)
                val bestResultIndex = ReturnBestSubjectIndex(population, matrix)
                bestCandidate = population[bestResultIndex].clone()
                println(bestResult)

                SaveBestOneToTxtFile(bestCandidate, matrix, "Best_${fileName}_Candidate.txt")
            }
        }
    }

}


// *******************************************************************
//          Metody do obslugi wczytania i zapisania do pliku
// *******************************************************************

// wczytuje plik (zwraca macierz, ustala dlugosc)
private fun ReadFile(filename: String): Array<IntArray> {

    var fileReader: FileReader? = null
    var bufferedReader: BufferedReader? = null
    var stringLine: String? //zmienna do wczytywania linii z pliku
    var numbers: Array<String> //tablica do oddzielania wartosci liczbowych (kazda linia) za pomoca split
    var row = 0
    var table:Array<IntArray>

    try {
        fileReader = FileReader(filename)
        bufferedReader = BufferedReader(fileReader)
        FILE_LENGTH = bufferedReader.readLine().toInt() // wczytuje
        table = Array(FILE_LENGTH) { IntArray(FILE_LENGTH) }

        for (str in bufferedReader.readLines()) // wczytuje kazda linie w pliku (oprocz pierwszej bo byla wczytana wczesniej - 2 linijki wyzej
        {
            stringLine = str.trim() // usuwa biale znaki
            numbers = stringLine.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray() // rozdziela kazda liczbe w linii patrzac na znak spacji " "

            for (column in numbers.indices) // indices sluzy do indeksowania,
            {
                table[row][column] = numbers[column].toInt()
                table[column][row] = numbers[column].toInt()
            }
            row++ // zmienna sluzaca do zwiekszania definicji wierszy w tablicy
        }
        return table
    }
    catch (e: FileNotFoundException)
    {
        println(e.toString())
        return (Array(0) { IntArray(0)})
    }
    finally
    {
        when
        {
            fileReader != null -> fileReader.close()
            bufferedReader != null -> bufferedReader.close()
        }
    }
}

//zapis najlepszego wyniku do pliku
private fun SaveBestOneToTxtFile(candidate: IntArray, matrix: Array<IntArray>, fileName: String) {

    var fileToSave: PrintWriter? = null
    try {
        fileToSave = PrintWriter(fileName)
        for (i in candidate.indices) {
            if (i == candidate.size - 1)
                fileToSave.printf("%d", candidate[i])
            else
                fileToSave.printf("%d-", candidate[i])
        }
        fileToSave.print(" " + CalculateBestCandidateDistance(candidate, matrix))
    } catch (io: IOException) {
        println("Nie udalo sie zapisac wyniku do pliku")
    } finally {
        when
        {
            fileToSave != null -> fileToSave.close()
        }
    }
}


// ****************************************************************************
//                          Glowne metody algorytmu
// ****************************************************************************

// obliczanie dystansu osobnikow wg macierzy
fun CalculateDistance(matrix: Array<IntArray>, population: Array<IntArray>) : IntArray
{
    val perRowSum = IntArray(population.size)

    for (secondTabRow in 0 until population.size) {
        perRowSum[secondTabRow] = 0

        for (secondTabColumn in 0 until population[secondTabRow].size) {
            if (secondTabColumn < population[secondTabRow].size - 1) {
                perRowSum[secondTabRow] += matrix[population[secondTabRow][secondTabColumn]][population[secondTabRow][secondTabColumn + 1]]
            } else {
                perRowSum[secondTabRow] += matrix[population[secondTabRow][secondTabColumn]][population[secondTabRow][0]]
            }
        }
    }
    return perRowSum
}

//generator losowych tras
fun RandomPopulationGenerator(length: Int, rowsCount: Int): Array<IntArray> {

    val random = Random()
    val tab = Array(rowsCount) { IntArray(length) }
    var liczba:Int

    for (row in 0 until rowsCount)
    {
        tab[row].fill(Int.MAX_VALUE,0,tab[row].size) // potrzebne aby wypelnic tablice wartosciami innymi niz losowane (psuje to petle while)

        for (column in 0 until length)
        {
            do
            {
                liczba = random.nextInt(length)
            } while (IntStream.of(*tab[row]).anyMatch { number -> number == liczba})

            tab[row][column] = liczba
        }
    }
    return tab
}

// metoda ktora losuje kilku osobnikow z populacji
private fun RandomPopulation(population: Array<IntArray>, amount: Int) : IntArray {

    val random = Random()
    var helper = 0
    var number: Int
    val randomNumbers = IntArray(amount)
    val candidatesToReturn = Array(amount) {IntArray(FILE_LENGTH)}

    randomNumbers.fill(Int.MAX_VALUE,0, amount)

    do {
        number = random.nextInt(population.size)
        if (!randomNumbers.contains(number)) {
            randomNumbers[helper] = number
            candidatesToReturn[helper] = population[number]
            helper++
        }
    } while (helper < amount)

    //println(Arrays.toString(randomNumbers))
    return randomNumbers
}

// metoda do turnieju
private fun Tournament (population: Array<IntArray>, matrix: Array<IntArray>, populationAmount: Int): Array<IntArray>
{
    val returnBestCandidates = Array(population.size) { IntArray(population[0].size) }
    var randomCandidatesIndexes: IntArray
    var candidate: Array<IntArray> = Array(1) { IntArray(52) }
    var index: Int
    var minValue:Int
    var minHelp: Int

    for (bestCandidates in 0 until population.size)
    {
        index = 0
        minValue = Int.MAX_VALUE
        randomCandidatesIndexes = RandomPopulation(population,populationAmount)
        for (chooseBestOne in 0 until populationAmount)
        {
            candidate[0] = (population[randomCandidatesIndexes[chooseBestOne]])
            minHelp = CalculateDistance(matrix,candidate)[0]

            if (minHelp < minValue)
            {
                minValue = minHelp
                index = randomCandidatesIndexes[chooseBestOne]
            }
        }
        returnBestCandidates[bestCandidates] = population[index]
    }
    return returnBestCandidates
}

// metoda do ruletki
private fun Roulette(population: Array<IntArray>, matrix: Array<IntArray>): Array<IntArray> {

    val bestRouletteIndexCandidate = IntArray(population.size)
    val populationAfterRoulette = Array(population.size) { IntArray(population[0].size) }
    val random = Random()
    val worstLength = WorstCandidateLength(population, matrix)
    val grades = IntArray(population.size)
    var sumGrades = 0
    var randomNumber: Int
    var sum: Int
    var index: Int
    for (i in population.indices) {
        grades[i] = worstLength + 1 - CalculateDistance(matrix,population)[i]
        sumGrades += grades[i]
    }

    for (i in bestRouletteIndexCandidate.indices) {
        randomNumber = random.nextInt(sumGrades)
        sum = 0
        index = 0
        sum += grades[index]
        while (sum <= randomNumber) {
            index++
            sum += grades[index]
        }
        bestRouletteIndexCandidate[i] = index
    }

    for (i in populationAfterRoulette.indices) {
        for (j in 0 until populationAfterRoulette[i].size) {
            populationAfterRoulette[i][j] = population[bestRouletteIndexCandidate[i]][j]
        }
    }

    return populationAfterRoulette
}

// metoda PMX
private fun CalculatePMX(parents: Array<IntArray>, chance: Int): Array<IntArray> {

    val parentLength = parents[0].size
    val random = Random()
    var chanceToPMX: Int
    var startBoundary: Int
    var endBoundary: Int
    var d1_tmp: Int
    var d2_tmp: Int
    var d1_tmp2: Int
    var d2_tmp2: Int
    var d1_index: Int
    var d2_index: Int
    var firstParentRange: IntArray
    var secondParentRange: IntArray
    val firstChild = IntArray(parents[0].size)
    val secondChild = IntArray(parents[0].size)
    var pmxResult: Array<IntArray> = Array(parents.size) { IntArray(parents[0].size)}
    var parentsCount = 1

    while (parentsCount < parents.size)
    //petla wykonujaca sie co 2 zaczynajac od drugiego parenta
    {
        chanceToPMX = random.nextInt(100)

        if (chance in 0..chanceToPMX)
        {
            //liczy granice
            do {
                startBoundary = random.nextInt(parentLength - 1) //gdy parent ma length = 52 to liczy od 0 do 51 (aby nie wyjsc poza granice tablicy)
                endBoundary = random.nextInt(parentLength - 1)
            } while (startBoundary >= endBoundary)

            firstParentRange = Arrays.copyOfRange(parents[parentsCount - 1], startBoundary, endBoundary + 1) //wyciaga elementy z tablicy miedzy granicami
            secondParentRange = Arrays.copyOfRange(parents[parentsCount], startBoundary, endBoundary + 1) //wyciaga elementy z tablicy miedzy granicami

            for (counter in 0 until parentLength) {
                if ((counter !in startBoundary..endBoundary)) {

                    //uzupelnia pozaobszarowe elementy
                    firstChild[counter] = parents[parentsCount - 1][counter] //przypisanie reszty wartosci z parenta 1 do child 1
                    secondChild[counter] = parents[parentsCount][counter] //przypisanie reszty wartosci z parenta 2 do child 2
                } else {
                    firstChild[counter] = parents[parentsCount][counter]
                    secondChild[counter] = parents[parentsCount - 1][counter]
                }
            }

            for (counter in startBoundary..endBoundary) {

                if (!ContainsInt(firstParentRange, parents[parentsCount][counter]))

                //sprawdza ktore elementy nie zostały skopiowane
                {
                    d1_tmp = parents[parentsCount][counter]
                    d1_tmp2 = parents[parentsCount - 1][counter]

                    do {
                        d1_index = ReturnIndex(parents[parentsCount], d1_tmp2)
                        d1_tmp2 = parents[parentsCount - 1][d1_index]
                    } while (d1_index >= startBoundary && d1_index <= endBoundary)
                    secondChild[d1_index] = d1_tmp
                }

                if (!ContainsInt(secondParentRange, parents[parentsCount - 1][counter])) {
                    d2_tmp = parents[parentsCount - 1][counter]
                    d2_tmp2 = parents[parentsCount][counter]

                    do {
                        d2_index = ReturnIndex(parents[parentsCount - 1], d2_tmp2)
                        d2_tmp2 = parents[parentsCount][d2_index]
                    } while (d2_index >= startBoundary && d2_index <= endBoundary)
                    firstChild[d2_index] = d2_tmp
                }
            }
            pmxResult[parentsCount-1] = firstChild
            pmxResult[parentsCount] = secondChild
        }
        else
        {
            pmxResult[parentsCount-1] = parents[parentsCount-1]
            pmxResult[parentsCount] = parents[parentsCount-1]
        }
        parentsCount += 2

    }
    return pmxResult
}

// metoda do mutacji
private fun Mutate(chanceToMutate: Int, _pmxResult: Array<IntArray>): Array<IntArray>
{
    val mutationResult = Array(_pmxResult.size) { IntArray(_pmxResult[0].size) }
    var startBoundary: Int
    var endBoundary: Int
    val random = Random()

    for (i in mutationResult.indices) {
        for (j in 0 until mutationResult[i].size) {
            mutationResult[i][j] = _pmxResult[i][j]
        }

    }
    for (i in _pmxResult.indices) {

        if (random.nextInt(100) < chanceToMutate) {
            startBoundary = random.nextInt(_pmxResult[i].size)
            endBoundary = random.nextInt(_pmxResult[i].size)

            while (startBoundary == endBoundary) {
                endBoundary = random.nextInt(_pmxResult[i].size)
            }

            if (endBoundary < startBoundary) {
                startBoundary = endBoundary
                endBoundary = startBoundary
            }

            var arrayToReverseValues = IntArray(endBoundary - startBoundary)
            for (j in arrayToReverseValues.indices) {
                arrayToReverseValues[j] = mutationResult[i][startBoundary + j]
            }

            arrayToReverseValues.reverse()

            for (j in arrayToReverseValues.indices) {
                mutationResult[i][startBoundary + j] = arrayToReverseValues[j]
            }
        }
    }
    return mutationResult
}

// *******************************************************************************
//                              Metody pomocnicze
// *******************************************************************************


// Metoda pomocnicza (gdy chcialem to zrobic na Array<IntArray> to wyskakiwal mi blad odnosnie formatu)
private fun CalculateBestCandidateDistance(candidate: IntArray, matrix: Array<IntArray>?): Int {
    var length = 0
    for (j in 0 until candidate.size - 1) {
        length += matrix!![candidate[j]][candidate[j + 1]]
    }
    length += matrix!![candidate[0]][candidate[candidate.size - 1]]
    return length
}

private fun WorstCandidateLength(population: Array<IntArray>, matrix: Array<IntArray>): Int {
    var worstLength = Integer.MIN_VALUE
    for (i in population.indices)
    {
        if (CalculateDistance(matrix,population)[i] > worstLength)
            worstLength = CalculateDistance(matrix,population)[i]
    }
    return worstLength
}

// zwraca index (potrzebne przy PMX do tworzenia dzieci
private fun ReturnIndex(array: IntArray, number: Int): Int {
    var counter = 0
    var returnIndex = 0

    for (x in array) {
        if (number == x) {
            returnIndex = counter
        }
        counter++
    }
    return returnIndex
}

// zwraca sume najlepszego osobnika z populacji
private fun ReturnBestSubjectLength(population: Array<IntArray>, matrix: Array<IntArray>): Int {
    var bestLength = Int.MAX_VALUE
    for (counteer in population.indices) {
        if (CalculateDistance(matrix,population)[counteer] < bestLength)
            bestLength = CalculateDistance(matrix,population)[counteer]
    }
    return bestLength
}


// zwraca index najlepszego osobnika z populacji
private fun ReturnBestSubjectIndex(population: Array<IntArray>, matrix: Array<IntArray>): Int {
    var bestLength = Int.MAX_VALUE
    var bestIndex = -1
    for (counter in population.indices) {
        if (CalculateDistance(matrix,population)[counter] < bestLength) {
            bestLength = CalculateDistance(matrix,population)[counter]
            bestIndex = counter
        }
    }
    return bestIndex
}

//sprawdza czy wartosc Int zawiera sie w tablicy
private fun ContainsInt(array: IntArray, number: Int): Boolean {
    var result = false

    for (x in array) {
        if (x == number) {
            result = true
        }
    }
    return result
}