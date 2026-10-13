"""Bounded completed-evidence reads on Linux and Windows; never follow final links."""
import os
from pathlib import Path
import stat


def windows_descriptor(path):
    """Open the reparse point itself and deny concurrent writes/deletion.

    CreateFileW's OPEN_REPARSE_POINT flag also rejects a final symlink swapped
    in between the preliminary path check and the actual open.
    """
    import ctypes
    from ctypes import wintypes as w
    import msvcrt

    class AttributeTag(ctypes.Structure):
        _fields_ = [("attributes", w.DWORD), ("tag", w.DWORD)]

    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.CreateFileW.argtypes = [w.LPCWSTR, w.DWORD, w.DWORD, ctypes.c_void_p,
                                  w.DWORD, w.DWORD, w.HANDLE]
    kernel.CreateFileW.restype = w.HANDLE
    kernel.GetFileType.argtypes = [w.HANDLE]
    kernel.GetFileType.restype = w.DWORD
    kernel.GetFileInformationByHandleEx.argtypes = [w.HANDLE, ctypes.c_int, ctypes.c_void_p, w.DWORD]
    kernel.GetFileInformationByHandleEx.restype = w.BOOL
    kernel.CloseHandle.argtypes = [w.HANDLE]
    kernel.CloseHandle.restype = w.BOOL
    # GENERIC_READ, FILE_SHARE_READ, OPEN_EXISTING, FILE_FLAG_OPEN_REPARSE_POINT.
    handle = kernel.CreateFileW(str(Path(path).absolute()), 0x80000000, 1, None, 3, 0x00200000, None)
    if handle == ctypes.c_void_p(-1).value:
        raise ctypes.WinError(ctypes.get_last_error())
    try:
        info = AttributeTag()
        if kernel.GetFileType(handle) != 1:
            raise ValueError("Evidence is not an ordinary disk file")
        if not kernel.GetFileInformationByHandleEx(handle, 9, ctypes.byref(info), ctypes.sizeof(info)):
            raise ctypes.WinError(ctypes.get_last_error())
        if info.attributes & (0x10 | 0x400):
            raise ValueError("Evidence directories and reparse points are forbidden")
        descriptor = msvcrt.open_osfhandle(handle, os.O_RDONLY | os.O_BINARY)
        handle = None  # Ownership transfers to the CRT descriptor.
        return descriptor
    finally:
        if handle is not None:
            kernel.CloseHandle(handle)


def read_text(path, limit):
    """Refuse devices, links, changing files and oversized inputs."""
    if type(limit) is not int or limit < 1:
        raise ValueError("Evidence size limit must be a positive integer")
    if os.name == "nt":
        info = os.lstat(path)
        if not stat.S_ISREG(info.st_mode) or getattr(info, "st_file_attributes", 0) & 0x400:
            raise ValueError("Evidence must be a bounded regular non-symlink file")
        fd = windows_descriptor(path)
    else:
        fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    with os.fdopen(fd, "rb") as source:
        before = os.fstat(source.fileno())
        if not stat.S_ISREG(before.st_mode) or before.st_size > limit:
            raise ValueError("Evidence must be a bounded regular non-symlink file")
        raw = source.read(limit + 1)
        after = os.fstat(source.fileno())
    if len(raw) > limit or any(getattr(before, key) != getattr(after, key) for key in
                             ("st_dev", "st_ino", "st_size", "st_mtime_ns", "st_ctime_ns")):
        raise ValueError("Evidence changed during read or exceeded its size bound")
    return raw.decode("utf-8-sig")
