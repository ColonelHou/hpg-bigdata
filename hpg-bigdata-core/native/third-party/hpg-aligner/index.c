#include <stdio.h>
#include <stdlib.h>
#include <string.h>

//----------------------------------------------------------------------------------------------------------------------
//
//----------------------------------------------------------------------------------------------------------------------

void *load_index(const char *index_path) {
    char *index = (char *) calloc(100, sizeof(char));
    sprintf(index, "index located at %s", index_path);
    printf("Loading index...\n\t%s\n...done!\n", index);
    return (void *) index;
}

//----------------------------------------------------------------------------------------------------------------------
//
//----------------------------------------------------------------------------------------------------------------------

void free_index(void *index) {
    if (index) {
        printf("Freeing index...\n\t%s\n...done!\n", (char *) index);
        free((char *) index);
    }
}
