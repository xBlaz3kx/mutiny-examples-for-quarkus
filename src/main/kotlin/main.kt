import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni

fun main(args: Array<String>) {
    databaseRead()
    combineListsAsMulti()
    sendJson()
    combineMultipleMultis()
}

fun databaseRead() {
    // Fetch two related database objects reactively (perform check).
    Uni.createFrom().item("Fetching database item").chain { container ->
        println(container)
        Uni.createFrom().item("Fetching another database item")
    }.chain { item ->
        println(item)
        Uni.createFrom().item("Adding to database")
    }.subscribe().with { println(it) }
}

fun multiArrayTest() {
    // Transform array to Multi stream
    Uni.createFrom().item(arrayListOf("Item1", "Item3", "Item2")).onItem()
        .transformToMulti { list -> Multi.createFrom().iterable(list) }.onItem()
        .transformToUniAndConcatenate { item -> Uni.createFrom().item(item) }.subscribe().with { item -> println(item) }
}

fun combineListsAsMulti() {
    // Combine multiple lists as a Multi object.
    Uni.createFrom().item(arrayListOf("Item1", "Item3", "Item2")).onItem().ifNotNull().transformToMulti { item ->
        Multi.createBy().combining()
            .streams(
                Multi.createFrom().iterable(item),
                Multi.createFrom().iterable(arrayListOf("Item4", "Item5", "Item6"))
            )
            .asTuple()
    }.onItem().transformToUni { tuple ->
        var item1 = tuple.item1
        var item2 = tuple.item2
        Uni.createFrom().item("Item 1 is $item1, item 2 is $item2")
    }.concatenate().subscribe().with { item -> println(item) }
}

fun combineMultipleMultis() {
    // Combine multiple Multi streams.
    Uni.createFrom().item(arrayListOf("Item1", "Item3", "Item2")).onItem().ifNotNull().transformToMulti { item ->
        Multi.createBy().combining()
            .streams(
                Multi.createFrom().iterable(item),
                Multi.createFrom().iterable(arrayListOf("Item4", "Item5", "Item6")),
                Multi.createFrom().iterable(arrayListOf("Item7", "Item8", "Item9"))
            ).using { item1, item2, item3 -> "$item1 $item2 $item3" }
    }.subscribe().with { item -> println(item) }
}

fun sendJson() {
    // Create transformations on a json object
    val messageStructure: HashMap<String, Any> = hashMapOf()
    val listOfShelves: ArrayList<HashMap<String, Any>> = arrayListOf()
    Multi.createFrom().items("Item1", "Item2", "Item3").onItem().transformToUniAndConcatenate { shelf ->
        Uni.combine().all().unis(
            Uni.createFrom().item(shelf),
            Uni.createFrom().item("Item4")
        ).asTuple()
    }.subscribe().with { tuple ->
        var shelf = tuple.item1
        var sector = tuple.item2
        println("shelf: $shelf , sector: $sector")
        val entry: HashMap<String, Any> = hashMapOf()
        entry["sectorId"] = sector
        entry["shelfId"] = shelf
        entry["rackId"] = shelf + "rackid"
        listOfShelves.add(entry)
    }
    messageStructure["shelves"] = listOfShelves
    println(jacksonObjectMapper().writeValueAsString(messageStructure))
}