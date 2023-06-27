#include "fstapi.h"

int getHierType(struct fstHier * hier){
	return hier->htyp;
}

void getHierScope(struct fstHier* h, struct fstHierScope* scope){
	if(h->htyp==FST_HT_SCOPE)
		*scope=h->u.scope;
}

void getHierVar(struct fstHier* h, struct fstHierVar* var){
	if(h->htyp==FST_HT_VAR)
		*var=h->u.var;
}

void getHierAttr(struct fstHier* h, struct fstHierAttr* attr){
	if(h->htyp==FST_HT_ATTRBEGIN)
		*attr=h->u.attr;
}

typedef void (*value_change_callback)(uint64_t time, fstHandle facidx, const char *value);

static void forward_cb(void *user_callback_data_pointer, uint64_t time, fstHandle facidx, const unsigned char *value) {
	//fprintf(stderr, "val: %s @ %ld\n", value, time);
	((value_change_callback)user_callback_data_pointer)(time, facidx, value);
}

void iterateValueChanges(void* ctx, value_change_callback vcc) {
	fstReaderIterBlocks(ctx, forward_cb, vcc, NULL);
}