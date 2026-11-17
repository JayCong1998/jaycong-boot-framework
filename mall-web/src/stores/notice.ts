import { create } from "zustand";
let timer: ReturnType<typeof setTimeout>;
export const useNoticeStore = create<{
  message: string;
  show: (text: string) => void;
}>((set) => ({
  message: "",
  show(text) {
    clearTimeout(timer);
    set({ message: text });
    timer = setTimeout(() => set({ message: "" }), 3200);
  },
}));
