import * as Dialog from "@radix-ui/react-dialog";
import { ShoppingBag, Minus, Plus, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useCartStore, cartCount, cartTotal } from "@/stores/cart";
import { money } from "@/lib/utils";
export default function CartSheet() {
  const cart = useCartStore(),
    count = useCartStore(cartCount),
    total = useCartStore(cartTotal);
  return (
    <Dialog.Root open={cart.open} onOpenChange={cart.setOpen}>
      <Dialog.Portal>
        <Dialog.Overlay
          data-slot="sheet-overlay"
          className="data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 fixed inset-0 z-50 bg-black/80"
        />
        <Dialog.Content
          data-slot="sheet-content"
          className="bg-background data-[state=open]:animate-in data-[state=closed]:animate-out fixed z-50 flex flex-col gap-4 shadow-lg transition ease-in-out data-[state=closed]:duration-300 data-[state=open]:duration-500 data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right inset-y-0 right-0 h-full w-3/4 border-l sm:max-w-sm cart-sheet"
          onCloseAutoFocus={(e) => {
            e.preventDefault();
            document
              .querySelector<HTMLButtonElement>('[aria-label="打开购物车"]')
              ?.focus();
          }}
        >
          <div data-slot="sheet-header" className="flex flex-col gap-1.5 p-4">
            <Dialog.Title
              data-slot="sheet-title"
              className="text-foreground font-semibold flex items-center gap-2"
            >
              <ShoppingBag size={21} />
              我的购物车{" "}
              <span className="text-sm text-muted-foreground">({count})</span>
            </Dialog.Title>
            <Dialog.Description
              data-slot="sheet-description"
              className="text-muted-foreground text-sm"
            >
              把喜欢的好物，留给自己的生活。
            </Dialog.Description>
          </div>
          {!cart.items.length ? (
            <div className="empty-state cart-empty">
              <ShoppingBag size={52} strokeWidth={1.2} />
              <h3>购物车还是空的</h3>
              <p>去发现让你心动的好物吧</p>
              <Button onClick={() => cart.setOpen(false)}>继续逛逛</Button>
            </div>
          ) : (
            <>
              <div className="cart-items">
                {cart.items.map((item) => (
                  <div key={item.product.id} className="cart-row">
                    <img src={item.product.image} alt={item.product.name} />
                    <div className="cart-item-main">
                      <h3>{item.product.name}</h3>
                      <span className="price">
                        ¥ {money(item.product.price)}
                      </span>
                      <div className="quantity-control">
                        <button
                          disabled={item.quantity <= 1}
                          aria-label={`减少${item.product.name}数量`}
                          onClick={() => cart.change(item.product.id, -1)}
                        >
                          <Minus size={13} />
                        </button>
                        <span>{item.quantity}</span>
                        <button
                          disabled={item.quantity >= 99}
                          aria-label={`增加${item.product.name}数量`}
                          onClick={() => cart.change(item.product.id, 1)}
                        >
                          <Plus size={13} />
                        </button>
                      </div>
                    </div>
                    <button
                      className="remove-item"
                      aria-label={`移除${item.product.name}`}
                      onClick={() => cart.remove(item.product.id)}
                    >
                      <Trash2 size={17} />
                    </button>
                  </div>
                ))}
              </div>
              <div className="cart-summary">
                <div>
                  <span>
                    合计 <small>共 {count} 件</small>
                  </span>
                  <strong className="price">¥ {money(total)}</strong>
                </div>
                <p>当前提供商品浏览与购物车，订单结算暂未开放。</p>
                <Button className="w-full" onClick={() => cart.setOpen(false)}>
                  继续选购
                </Button>
              </div>
            </>
          )}
          <Dialog.Close className="ring-offset-background focus:ring-ring data-[state=open]:bg-secondary absolute top-4 right-4 rounded-xs opacity-70 transition-opacity hover:opacity-100 focus:ring-2 focus:ring-offset-2 focus:outline-hidden disabled:pointer-events-none">
            <X className="size-4" />
            <span className="sr-only">关闭</span>
          </Dialog.Close>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
