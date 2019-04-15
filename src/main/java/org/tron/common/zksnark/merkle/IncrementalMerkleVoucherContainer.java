package org.tron.common.zksnark.merkle;

import java.util.ArrayDeque;
import java.util.Deque;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract.OutputPoint;
import org.tron.protos.Contract.PedersenHash;

public class IncrementalMerkleVoucherContainer {

  public static Integer DEPTH = IncrementalMerkleTreeContainer.DEPTH;

  private IncrementalMerkleVoucherCapsule voucherCapsule;

  public IncrementalMerkleVoucherContainer(IncrementalMerkleVoucherCapsule voucherCapsule) {
    this.voucherCapsule = voucherCapsule;
  }

  public IncrementalMerkleVoucherContainer(IncrementalMerkleTreeContainer tree) {
    this.voucherCapsule = new IncrementalMerkleVoucherCapsule();
    this.voucherCapsule.setTree(tree.getTreeCapsule());
  }

  private Deque<PedersenHash> partialPath() {

    Deque<PedersenHash> uncles = new ArrayDeque<>(voucherCapsule.getFilled());

    if (cursorExist()) {
      uncles.add(
          voucherCapsule.getCursor().toMerkleTreeContainer().root(voucherCapsule.getCursorDepth()));
    }

    return uncles;
  }

  public void append(PedersenHash obj) {

    if (cursorExist()) {
      IncrementalMerkleTreeCapsule cursor = voucherCapsule.getCursor();
      cursor.toMerkleTreeContainer().append(obj);
      voucherCapsule.setCursor(cursor);

      long cursorDepth = voucherCapsule.getCursorDepth();

      if (voucherCapsule.getCursor().toMerkleTreeContainer().isComplete(cursorDepth)) {
        voucherCapsule.addFilled(
            voucherCapsule.getCursor().toMerkleTreeContainer().root(cursorDepth));
        voucherCapsule.clearCursor();
      }
    } else {
      long nextDepth =
          voucherCapsule
              .getTree()
              .toMerkleTreeContainer()
              .nextDepth(voucherCapsule.getFilled().size());

      voucherCapsule.setCursorDepth(nextDepth);

      if (nextDepth >= DEPTH) {
        throw new RuntimeException("tree is full");
      }

      if (nextDepth == 0) {
        voucherCapsule.addFilled(obj);
      } else {
        IncrementalMerkleTreeCapsule cursor = new IncrementalMerkleTreeCapsule();
        cursor.toMerkleTreeContainer().append(obj);
        voucherCapsule.setCursor(cursor);
      }
    }
  }

  public IncrementalMerkleVoucherCapsule getVoucherCapsule() {
    return voucherCapsule;
  }

  public MerklePath path() {
    return voucherCapsule.getTree().toMerkleTreeContainer().path(partialPath());
  }

  public PedersenHash element() {
    return voucherCapsule.getTree().toMerkleTreeContainer().last();
  }

  public long position() {
    return voucherCapsule.getTree().toMerkleTreeContainer().size() - 1;
  }

  public PedersenHash root() {
    return voucherCapsule.getTree().toMerkleTreeContainer().root(DEPTH, partialPath());
  }

  public byte[] getMerkleVoucherKey() {
    OutputPoint outputPoint = voucherCapsule.getOutputPoint();

    if (outputPoint.getHash().isEmpty()) {
      throw new RuntimeException("outputPoint is not initialized");
    }
    return OutputPointUtil.outputPointToKey(outputPoint);
  }

  public byte[] getRootArray() {
    return root().getContent().toByteArray();
  }

  private boolean cursorExist() {
    return !voucherCapsule.getCursor().isEmptyTree();
  }

  public static class OutputPointUtil {

    public static byte[] outputPointToKey(OutputPoint outputPoint) {
      return outputPointToKey(outputPoint.getHash().toByteArray(), outputPoint.getIndex());
    }

    public static byte[] outputPointToKey(byte[] hashBytes, int index) {
      byte[] indexBytes = ByteArray.fromInt(index);
      byte[] rs = new byte[hashBytes.length + indexBytes.length];
      System.arraycopy(hashBytes, 0, rs, 0, hashBytes.length);
      System.arraycopy(indexBytes, 0, rs, hashBytes.length, indexBytes.length);
      return rs;
    }
  }

  public int size() {
    return voucherCapsule.getTree().toMerkleTreeContainer().size()
        + voucherCapsule.getFilled().size()
        + voucherCapsule.getCursor().toMerkleTreeContainer().size();
  }

  //for test only
  public void printSize() {
    System.out.println(
        "TreeSize:"
            + voucherCapsule.getTree().toMerkleTreeContainer().size()
            + ",FillSize:"
            + voucherCapsule.getFilled().size()
            + ",CursorSize:"
            + voucherCapsule.getCursor().toMerkleTreeContainer().size());
  }
}
