package net.immute.ccs.impl;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Hamt<K, V> {
    private static final TrieNode EMPTY_NODE = new TrieNode(0, 0, new Object[0]);

    private final Node<K, V> root;

    public Hamt() {
        //noinspection unchecked
        this(EMPTY_NODE);
    }

    private Hamt(Node<K, V> root) {
        this.root = root;
    }

    public V get(K key) {
        return root.find(key, key.hashCode(), 0);
    }

    public Hamt<K, V> update(K key, Function<V, V> newValue) {
        int hash = key.hashCode();
        Node<K, V> newRoot = root.updated(key, newValue, hash, 0);
        if (newRoot != root)
            return new Hamt<>(newRoot);
        return this;
    }

    public void forEach(BiConsumer<K, V> f) {
        root.forEach(f);
    }

    private interface Node<K, V> {
        V find(K key, int hash, int shift);
        Node<K, V> updated(K key, Function<V, V> newValue, int hash, int shift);
        void forEach(BiConsumer<K, V> f);
    }

    private static class TrieNode<K, V> implements Node<K, V> {
        private static final int TUPLE_LENGTH = 2;
        private static final int HASH_CODE_LENGTH = 32;
        private static final int BIT_PARTITION_SIZE = 5;
        private static final int BIT_PARTITION_MASK = 0b11111;

        final int nodeMap;
        final int dataMap;
        final Object[] children;

        TrieNode(int nodeMap, int dataMap, Object[] children) {
            this.nodeMap = nodeMap;
            this.dataMap = dataMap;
            this.children = children;
        }

        static int mask(final int keyHash, final int shift) {
            return (keyHash >>> shift) & BIT_PARTITION_MASK;
        }

        static int bitpos(final int mask) {
            return 1 << mask;
        }

        int dataIndex(final int bitpos) {
            return java.lang.Integer.bitCount(dataMap & (bitpos - 1));
        }

        K getKey(final int index) {
            //noinspection unchecked
            return (K)children[TUPLE_LENGTH * index];
        }

        V getValue(final int index) {
            //noinspection unchecked
            return (V)children[TUPLE_LENGTH * index + 1];
        }

        int nodeIndex(final int bitpos) {
            return java.lang.Integer.bitCount(nodeMap & (bitpos - 1));
        }

        Node<K, V> getNode(final int index) {
            //noinspection unchecked
            return (Node)children[children.length - 1 - index];
        }

        Node<K, V> nodeAt(final int bitpos) {
            return getNode(nodeIndex(bitpos));
        }

        @Override
        public void forEach(BiConsumer<K, V> f) {
            int dataArity = Integer.bitCount(dataMap);
            for (int i = 0; i < dataArity; i++)
                f.accept(getKey(i), getValue(i));

            int nodeArity = Integer.bitCount(nodeMap);
            for (int i = 0; i < nodeArity; i++)
                getNode(i).forEach(f);
        }

        @Override
        public V find(K key, int hash, int shift) {
            int mask = mask(hash, shift);
            int bitpos = bitpos(mask);

            if ((dataMap & bitpos) != 0) {
                int index = dataIndex(bitpos);
                if (getKey(index).equals(key))
                    return getValue(index);
                return null;
            }

            if ((nodeMap & bitpos) != 0) {
                return nodeAt(bitpos).find(key, hash, shift + BIT_PARTITION_SIZE);
            }

            return null;
        }

        @Override
        public Node<K, V> updated(K key, Function<V, V> newValue, int hash, int shift) {
            int mask = mask(hash, shift);
            int bitpos = bitpos(mask);

            if ((dataMap & bitpos) != 0) {
                int dataIndex = dataIndex(bitpos);
                K currentKey = getKey(dataIndex);

                if (currentKey.equals(key)) {
                    V currentVal = getValue(dataIndex);
                    V nextVal = newValue.apply(currentVal);

                    if (nextVal == currentVal)
                        return this;
                    return copyAndSetValue(bitpos, nextVal);
                } else {
                    V currentVal = getValue(dataIndex);
                    V nextVal = newValue.apply(null);

                    Node<K, V> subNodeNew =
                            mergeTwoKeyValPairs(currentKey, currentVal, currentKey.hashCode(),
                                    key, nextVal, hash, shift + BIT_PARTITION_SIZE);

                    return copyAndMigrateFromInlineToNode(bitpos, subNodeNew);
                }
            } else if ((nodeMap & bitpos) != 0) {
                Node<K, V> subNode = nodeAt(bitpos);
                Node<K, V> subNodeNew = subNode.updated(key, newValue, hash, shift + BIT_PARTITION_SIZE);

                if (subNodeNew == subNode)
                    return this;
                return copyAndSetNode(bitpos, subNodeNew);
            } else {
                V nextVal = newValue.apply(null);
                return copyAndInsertValue(bitpos, key, nextVal);
            }
        }

        private Node<K, V> mergeTwoKeyValPairs(K key0, V val0, int hash0,
                                               K key1, V val1, int hash1, int shift) {
            assert !(key0.equals(key1));

            if (shift >= HASH_CODE_LENGTH) {
                //noinspection unchecked
                return new CollisionNode<>(hash0, (K[]) new Object[]{key0, key1},
                        (V[]) new Object[]{val0, val1});
            }

            final int mask0 = mask(hash0, shift);
            final int mask1 = mask(hash1, shift);

            if (mask0 != mask1) {
                final int dataMap = bitpos(mask0) | bitpos(mask1);
                if (mask0 < mask1) {
                    return new TrieNode<>(0, dataMap, new Object[]{key0, val0, key1, val1});
                } else {
                    return new TrieNode<>(0, dataMap, new Object[]{key1, val1, key0, val0});
                }
            } else {
                Node<K, V> node = mergeTwoKeyValPairs(key0, val0, hash0, key1, val1, hash1, shift + BIT_PARTITION_SIZE);
                final int nodeMap = bitpos(mask0);
                return new TrieNode<>(nodeMap, 0, new Object[]{node});
            }
        }

        private TrieNode<K, V> copyAndMigrateFromInlineToNode(int bitpos, Node<K, V> node) {
            int idxOld = TUPLE_LENGTH * dataIndex(bitpos);
            int idxNew = children.length - TUPLE_LENGTH - nodeIndex(bitpos);
            final Object[] dst = new Object[children.length - 1];
            System.arraycopy(children, 0, dst, 0, idxOld);
            System.arraycopy(children, idxOld + 2, dst, idxOld, idxNew - idxOld);
            dst[idxNew] = node;
            System.arraycopy(children, idxNew + 2, dst, idxNew + 1, children.length - idxNew - 2);
            return new TrieNode<>(nodeMap | bitpos, dataMap ^ bitpos, dst);
        }

        private TrieNode<K, V> copyAndInsertValue(int bitpos, K key, V val) {
            int idx = TUPLE_LENGTH * dataIndex(bitpos);
            Object[] dst = new Object[children.length + 2];
            System.arraycopy(children, 0, dst, 0, idx);
            dst[idx] = key;
            dst[idx + 1] = val;
            System.arraycopy(children, idx, dst, idx + 2, children.length - idx);
            return new TrieNode<>(nodeMap, dataMap | bitpos, dst);
        }

        private TrieNode<K, V> copyAndSetNode(int bitpos, Node<K, V> node) {
            int idx = children.length - 1 - nodeIndex(bitpos);
            Object[] dst = new Object[children.length];
            System.arraycopy(children, 0, dst, 0, children.length);
            dst[idx] = node;
            return new TrieNode<>(nodeMap, dataMap, dst);
        }

        private TrieNode<K, V> copyAndSetValue(int bitpos, V val) {
            int idx = TUPLE_LENGTH * dataIndex(bitpos) + 1;
            Object[] dst = new Object[children.length];
            System.arraycopy(children, 0, dst, 0, children.length);
            dst[idx] = val;
            return new TrieNode<>(nodeMap, dataMap, dst);
        }

        private static class CollisionNode<K, V> implements Node<K, V> {
            private final int hash;
            private final K[] keys;
            private final V[] values;

            CollisionNode(int hash, K[] keys, V[] values) {
                this.hash = hash;
                this.keys = keys;
                this.values = values;
            }

            @Override
            public void forEach(BiConsumer<K, V> f) {
                for (int i = 0; i < keys.length; i++)
                    f.accept(keys[i], values[i]);
            }

            @Override
            public V find(K key, int hash, int shift) {
                for (int i = 0; i < keys.length; i++)
                    if (key.equals(keys[i]))
                        return values[i];
                return null;
            }

            @Override
            public Node<K, V> updated(K key, Function<V, V> newValue, int hash, int shift) {
                assert this.hash == hash;

                for (int idx = 0; idx < keys.length; idx++) {
                    if (key.equals(keys[idx])) {
                        V currentVal = values[idx];
                        V nextVal = newValue.apply(currentVal);

                        if (nextVal == currentVal)
                            return this;
                        Object[] dst = new Object[values.length];
                        System.arraycopy(values, 0, dst, 0, values.length);
                        dst[idx] = nextVal;
                        //noinspection unchecked
                        return new CollisionNode<>(hash, keys, (V[])dst);
                    }
                }

                Object[] keysNew = new Object[keys.length + 1];
                System.arraycopy(keys, 0, keysNew, 0, keys.length);
                keysNew[keys.length] = key;

                V nextVal = newValue.apply(null);
                Object[] valsNew = new Object[values.length + 1];
                System.arraycopy(values, 0, valsNew, 0, values.length);
                valsNew[values.length] = nextVal;

                //noinspection unchecked
                return new CollisionNode<>(hash, (K[])keysNew, (V[])valsNew);
            }
        }
    }
}
