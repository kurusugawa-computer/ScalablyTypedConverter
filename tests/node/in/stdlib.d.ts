
declare interface Array<T> {
}

/**
 * Obtain the return type of a function type
 */
type ReturnType<T extends (...args: any[]) => any> = T extends (...args: any[]) => infer R ? R : any;

// from lib.es2015.iterable.d.ts
interface SymbolConstructor {
    readonly iterator: unique symbol;
}

// from lib.es2015.symbol.d.ts 
declare var Symbol: SymbolConstructor;

// from lib.es2015.iterable.d.ts
interface Iterator<T, TReturn = any, TNext = undefined> {
}