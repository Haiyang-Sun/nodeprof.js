async function foo(p) {
    let res;
    try {
        res = await p;
        console.log(res);
    } catch (e) {
        res = 'exception';
        console.log('exception', e);
    }
    return res;
}

(async(p)=>await foo(p))(Promise.resolve(44));
(async(p)=>await foo(p))(Promise.reject(-1));
(async(p)=>await foo(p))(43);
(async(p)=>await foo(p))(new Promise((res)=>{
    setTimeout(()=>{
        res("timeout");
    }, 1000);
}));
